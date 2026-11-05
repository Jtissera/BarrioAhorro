
package com.barrioahorro.api.modules.sorteos

import com.barrioahorro.api.modules.explore.ExploreService
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.roundToInt

@Service
class ClientRaffleService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val explore: ExploreService,
) {

    companion object {
        const val UNLOCK_POINTS = 100
        const val MAX_TICKETS_PER_PURCHASE = 10

        /** Sorteo abierto a la compra, con premio ratificado y aprobado por el admin. */
        private const val RAFFLE_IS_OPEN = """
            s.estado = 'ACTIVO'
            AND s.fecha_sorteo > now()
            AND cs.estado = 'RATIFICADO'
            AND cs.estado_revision = 'APROBADO'
        """

        private const val RAFFLE_FROM = """
            sorteos s
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cs.comercio_id
        """
    }

    private val visible = ExploreService.BUSINESS_IS_VISIBLE.trimIndent()

    private data class Profile(
        val balance: Int,
        val lifetime: Int,
        val unseen: Boolean
    )

    @Transactional(readOnly = true)
    fun overview(userId: Long): ClientRafflesResponse {
        explore.requireClient(userId)
        val profile = loadProfile(userId)
        val unlocked = profile.lifetime >= UNLOCK_POINTS

        return ClientRafflesResponse(
            unlocked = unlocked,
            unlockThreshold = UNLOCK_POINTS,
            lifetimePoints = profile.lifetime,
            balance = profile.balance,
            showUnlockAnimation = unlocked && profile.unseen,
            // Bloqueado: no se revela el listado.
            raffles = if (unlocked) loadRaffles(userId, null) else emptyList(),
        )
    }

    @Transactional
    fun markUnlockSeen(userId: Long) {
        explore.requireClient(userId)

        jdbc.update(
            """
            UPDATE perfiles_cliente
            SET sorteos_desbloqueo_visto_en = now()
            WHERE usuario_id = :id
              AND puntos_totales_historicos >= :min
              AND sorteos_desbloqueo_visto_en IS NULL
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("min", UNLOCK_POINTS),
        )
    }

    @Transactional
    fun buyTicket(
        userId: Long,
        raffleId: Long,
        quantity: Int = 1
    ): BuyTicketResponse {
        if (quantity !in 1..MAX_TICKETS_PER_PURCHASE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Podés comprar entre 1 y $MAX_TICKETS_PER_PURCHASE tickets por vez",
            )
        }

        explore.requireClient(userId)

        val cost = jdbc.query(
            """
            SELECT s.costo_puntos_ticket
            FROM $RAFFLE_FROM
            WHERE s.id = :id AND $RAFFLE_IS_OPEN AND ($visible)
            FOR SHARE OF s
            """.trimIndent(),
            MapSqlParameterSource("id", raffleId),
        ) { rs, _ -> rs.getInt(1) }.firstOrNull()
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Este sorteo ya no está disponible"
            )

        val profile = loadProfile(userId, lock = true)

        if (profile.lifetime < UNLOCK_POINTS) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Los sorteos todavía están bloqueados"
            )
        }

        if (profile.balance < cost * quantity) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "No te alcanzan los puntos para $quantity tickets"
            )
        }

        // Inserta los tickets y recupera sus IDs.
        // El trigger fija precio y validez, y descuenta en el ledger.
        val ticketIds = jdbc.queryForList(
            """
            INSERT INTO tickets_sorteo (sorteo_id, cliente_id, puntos_costo)
            SELECT CAST(:s AS bigint), CAST(:c AS bigint), CAST(:cost AS integer)
            FROM generate_series(1, CAST(:q AS integer))
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("s", raffleId)
                .addValue("c", userId)
                .addValue("cost", cost)
                .addValue("q", quantity),
            Long::class.java,
        ).filterNotNull().sorted()

        return BuyTicketResponse(
            balance = loadProfile(userId).balance,
            raffle = loadRaffles(userId, raffleId).first(),
            quantity = quantity,
            ticketNumbers = ticketIds,
            receiptCode = "TX-${ticketIds.first()}",
        )
    }

    @Transactional(readOnly = true)
    fun myTickets(userId: Long): MyTicketsResponse {
        explore.requireClient(userId)

        // Sin filtro de visibilidad: el historial se muestra aunque el comercio ya no esté visible.
        val items = jdbc.query(
            """
        SELECT s.id,
               s.estado::text AS estado_sorteo,
               s.fecha_sorteo,
               cs.premio_nombre,
               cs.premio_descripcion,
               cs.premio_foto_url,
               pc.nombre_negocio,
               COUNT(t.id) AS mios,
               COALESCE(SUM(t.puntos_costo), 0) AS gastados,
               COUNT(t.id) FILTER (WHERE t.estado = 'GANADOR') AS ganados,

               (
                   SELECT COUNT(*)
                   FROM tickets_sorteo x
                   WHERE x.sorteo_id = s.id
                     AND x.estado = 'VIGENTE'
               ) AS total_vigentes,

               array_agg(t.id ORDER BY t.id) AS numeros,

               -- Ticket que obtuvo el premio en este sorteo.
               (
                   SELECT p.ticket_id
                   FROM premios_sorteo p
                   WHERE p.sorteo_id = s.id
                     AND p.cliente_id = :userId
               ) AS ticket_premio,

               -- Estado actual del premio.
               (
                   SELECT CASE
                       WHEN p.canjeado_en IS NOT NULL THEN 'CANJEADO'
                       WHEN p.vence_en <= now() THEN 'VENCIDO'
                       ELSE 'CANJEABLE'
                   END
                   FROM premios_sorteo p
                   WHERE p.sorteo_id = s.id
                     AND p.cliente_id = :userId
               ) AS estado_premio

        FROM tickets_sorteo t
        JOIN sorteos s
            ON s.id = t.sorteo_id
        JOIN compromisos_sorteo cs
            ON cs.id = s.compromiso_sorteo_id
        JOIN perfiles_comercio pc
            ON pc.usuario_id = cs.comercio_id

        WHERE t.cliente_id = :userId

        GROUP BY s.id, cs.id, pc.usuario_id

        ORDER BY
            (s.estado::text = 'ACTIVO') DESC,
            s.fecha_sorteo DESC,
            s.id DESC
        """.trimIndent(),
            MapSqlParameterSource("userId", userId),
        ) { rs, _ ->

            val status = rs.getString("estado_sorteo")
            val mine = rs.getInt("mios")

            val winningTicketId = rs.getLong("ticket_premio")
                .takeIf { !rs.wasNull() }

            MyRaffleTicketsResponse(
                raffleId = rs.getLong("id"),
                raffleStatus = status,

                prizeName = rs.getString("premio_nombre")
                    ?.takeIf { it.isNotBlank() }
                    ?: rs.getString("premio_descripcion"),

                prizeDescription = rs.getString("premio_descripcion")
                    ?.takeIf { it.isNotBlank() },

                prizePhotoUrl = rs.getString("premio_foto_url")
                    ?.takeIf { it.isNotBlank() },

                businessName = rs.getString("nombre_negocio"),

                drawAt = rs.getObject(
                    "fecha_sorteo",
                    OffsetDateTime::class.java
                ),

                myTickets = mine,

                ticketNumbers = (rs.getArray("numeros").array as Array<*>)
                    .map { (it as Number).toLong() },

                pointsSpent = rs.getInt("gastados"),

                won = rs.getInt("ganados") > 0,

                oneIn = if (status == "ACTIVO") {
                    oneIn(rs.getInt("total_vigentes"), mine)
                } else {
                    null
                },

                winningTicketId = winningTicketId,

                prizeStatus = rs.getString("estado_premio"),
            )
        }

        return MyTicketsResponse(
            activeCount = items.count {
                it.raffleStatus == "ACTIVO"
            },
            items = items,
        )
    }

    // ---- Helpers --------------------------------------------------------------------

    private fun loadProfile(
        userId: Long,
        lock: Boolean = false
    ): Profile =
        jdbc.query(
            """
            SELECT saldo_puntos, puntos_totales_historicos,
                   sorteos_desbloqueo_visto_en IS NULL AS no_visto
            FROM perfiles_cliente WHERE usuario_id = :id
            """.trimIndent() + if (lock) " FOR UPDATE" else "",
            MapSqlParameterSource("id", userId),
        ) { rs, _ ->
            Profile(
                rs.getInt("saldo_puntos"),
                rs.getInt("puntos_totales_historicos"),
                rs.getBoolean("no_visto")
            )
        }.firstOrNull()
            ?: throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo los vecinos pueden participar de sorteos"
            )

    private fun loadRaffles(
        userId: Long,
        onlyId: Long?
    ): List<ClientRaffleResponse> {
        val params = MapSqlParameterSource("userId", userId)

        val idClause = if (onlyId != null) {
            params.addValue("raffleId", onlyId)
            "AND s.id = :raffleId"
        } else ""

        return jdbc.query(
            """
            SELECT s.id, cs.premio_nombre, cs.premio_descripcion, cs.premio_foto_url,
                   pc.usuario_id AS comercio_id, pc.nombre_negocio,
                   s.costo_puntos_ticket, s.fecha_sorteo,
                   COUNT(t.id) AS total,
                   COUNT(DISTINCT t.cliente_id) AS participantes,
                   COUNT(t.id) FILTER (WHERE t.cliente_id = :userId) AS mios
            FROM $RAFFLE_FROM
            LEFT JOIN tickets_sorteo t ON t.sorteo_id = s.id AND t.estado = 'VIGENTE'
            WHERE $RAFFLE_IS_OPEN AND ($visible) $idClause
            GROUP BY s.id, cs.id, pc.usuario_id
            ORDER BY s.fecha_sorteo ASC, s.id
            """.trimIndent(),
            params,
        ) { rs, _ ->
            val total = rs.getInt("total")
            val mine = rs.getInt("mios")

            ClientRaffleResponse(
                id = rs.getLong("id"),
                prizeName = rs.getString("premio_nombre")?.takeIf { it.isNotBlank() }
                    ?: rs.getString("premio_descripcion"),
                prizeDescription = rs.getString("premio_descripcion")?.takeIf { it.isNotBlank() },
                prizePhotoUrl = rs.getString("premio_foto_url")?.takeIf { it.isNotBlank() },
                businessId = rs.getLong("comercio_id"),
                businessName = rs.getString("nombre_negocio"),
                ticketCost = rs.getInt("costo_puntos_ticket"),
                drawAt = rs.getObject("fecha_sorteo", OffsetDateTime::class.java),
                totalTickets = total,
                participants = rs.getInt("participantes"),
                myTickets = mine,
                oneIn = oneIn(total, mine),
            )
        }
    }

    /** Sin tickets propios: lo que tendrías con uno nuevo. Con tickets: total / míos. */
    private fun oneIn(total: Int, mine: Int): Int =
        if (mine == 0) total + 1
        else max(1, (total.toDouble() / mine).roundToInt())
}
