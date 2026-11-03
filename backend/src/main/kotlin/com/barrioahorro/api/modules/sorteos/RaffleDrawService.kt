package com.barrioahorro.api.modules.sorteos

import com.barrioahorro.api.modules.explore.ExploreService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.security.SecureRandom
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class RaffleDrawService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val tx: TransactionTemplate,
    @Value("\${sorteos.max-por-mes:3}") private val maxPerMonth: Int,
    @Value("\${sorteos.dias-minimos-venta:7}") private val minSaleDays: Long,
) {
    private val log = LoggerFactory.getLogger(RaffleDrawService::class.java)
    private val random = SecureRandom()

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
        const val TICKET_COST_POINTS = 30
        private const val CLOSE_HOUR = 20

        /** Último día del mes a las 20:00 (hora de Buenos Aires). */
        fun closeOf(month: YearMonth): ZonedDateTime =
            month.atEndOfMonth().atTime(CLOSE_HOUR, 0).atZone(ZONE)

        /** Próximo cierre que deje al menos [minDays] días de venta. */
        fun nextClose(now: ZonedDateTime, minDays: Long): ZonedDateTime {
            var month = YearMonth.from(now)
            while (true) {
                val close = closeOf(month)
                if (close.isAfter(now.plusDays(minDays))) return close
                month = month.plusMonths(1)
            }
        }
    }

    // ---- Programación ---------------------------------------------------------------

    /**
     * Arma el sorteo del próximo cierre con los premios ratificados y aprobados que sigan
     * vivos ese día. Entran primero los que vencen antes; los que no llegarían al cierre
     * siguiente entran aunque se pase el cupo mensual. Idempotente.
     */
    fun programNextDraw(): Int = tx.execute { programInTx() } ?: 0

    private fun programInTx(): Int {
        jdbc.query(
            "SELECT pg_advisory_xact_lock(hashtextextended('sorteos:programar', 0))",
            MapSqlParameterSource(),
            RowCallbackHandler { },
        )

        val close = nextClose(ZonedDateTime.now(ZONE), minSaleDays)
        val closeAfter = closeOf(YearMonth.from(close).plusMonths(1))

        val alreadyScheduled = jdbc.queryForObject(
            "SELECT COUNT(*) FROM sorteos WHERE fecha_sorteo = :close AND estado::text IN ('ACTIVO','CERRADO','SORTEADO')",
            MapSqlParameterSource("close", close.toOffsetDateTime()),
            Int::class.java,
        ) ?: 0
        val freeSlots = (maxPerMonth - alreadyScheduled).coerceAtLeast(0)

        val candidates = jdbc.query(
            """
            SELECT cs.id, cs.ciclo_fin
            FROM compromisos_sorteo cs
            JOIN perfiles_comercio pc ON pc.usuario_id = cs.comercio_id
            WHERE cs.estado = 'RATIFICADO'
              AND cs.estado_revision = 'APROBADO'
              AND :closeDate BETWEEN cs.ciclo_inicio AND cs.ciclo_fin
              AND NOT EXISTS (SELECT 1 FROM sorteos s WHERE s.compromiso_sorteo_id = cs.id)
              AND (${ExploreService.BUSINESS_IS_VISIBLE.trimIndent()})
            ORDER BY cs.ciclo_fin ASC, cs.id
            """.trimIndent(),
            MapSqlParameterSource("closeDate", close.toLocalDate()),
        ) { rs, _ -> rs.getLong("id") to rs.getObject("ciclo_fin", LocalDate::class.java) }

        val urgentBefore = closeAfter.toLocalDate()
        val selected = candidates.filterIndexed { index, (_, end) -> index < freeSlots || end < urgentBefore }

        var created = 0
        for ((commitmentId, _) in selected) {
            created += jdbc.update(
                """
                INSERT INTO sorteos (compromiso_sorteo_id, costo_puntos_ticket, fecha_sorteo)
                VALUES (:id, :cost, :close)
                ON CONFLICT (compromiso_sorteo_id) DO NOTHING
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("id", commitmentId)
                    .addValue("cost", TICKET_COST_POINTS)
                    .addValue("close", close.toOffsetDateTime()),
            )
        }
        if (created > 0) log.info("Sorteos programados para {}: {}", close, created)
        return created
    }

    // ---- Ejecución ------------------------------------------------------------------

    fun executeDue(): Int {
        val ids = jdbc.queryForList(
            "SELECT id FROM sorteos WHERE estado::text = 'ACTIVO' AND fecha_sorteo <= now() ORDER BY fecha_sorteo, id",
            MapSqlParameterSource(),
            Long::class.java,
        ).filterNotNull()

        var done = 0
        for (id in ids) {
            try {
                if (tx.execute { drawInTx(id) } == true) done++
            } catch (e: Exception) {
                log.error("No se pudo ejecutar el sorteo {}", id, e)
            }
        }
        return done
    }

    /** SKIP LOCKED: si otra instancia o una compra en curso lo tiene tomado, se reintenta en la próxima corrida. */
    private fun drawInTx(id: Long): Boolean {
        val params = MapSqlParameterSource("id", id)

        val (commitment, review) = jdbc.query(
            """
            SELECT cs.estado::text AS compromiso, cs.estado_revision::text AS revision
            FROM sorteos s
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            WHERE s.id = :id AND s.estado::text = 'ACTIVO' AND s.fecha_sorteo <= now()
            FOR UPDATE OF s SKIP LOCKED
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.getString("compromiso") to rs.getString("revision") }.firstOrNull()
            ?: return false

        if (commitment != "RATIFICADO" || review != "APROBADO") {
            cancel(id, params)
            return true
        }

        val tickets = jdbc.queryForList(
            "SELECT id FROM tickets_sorteo WHERE sorteo_id = :id AND estado = 'VIGENTE' ORDER BY id",
            params,
            Long::class.java,
        ).filterNotNull()

        if (tickets.isEmpty()) {
            jdbc.update("UPDATE sorteos SET estado = 'DESIERTO', ejecutado_en = now(), total_tickets = 0 WHERE id = :id", params)
            log.info("Sorteo {} desierto", id)
            return true
        }

        // Todos los tickets tienen la misma probabilidad.
        val winner = tickets[random.nextInt(tickets.size)]
        params.addValue("winner", winner).addValue("n", tickets.size)
        jdbc.update(
            """
            UPDATE tickets_sorteo
            SET estado = CASE WHEN id = :winner THEN 'GANADOR' ELSE 'NO_GANADOR' END
            WHERE sorteo_id = :id AND estado = 'VIGENTE'
            """.trimIndent(),
            params,
        )
        jdbc.update(
            """
            UPDATE sorteos
            SET estado = 'SORTEADO', ejecutado_en = now(), ticket_ganador_id = :winner, total_tickets = :n
            WHERE id = :id
            """.trimIndent(),
            params,
        )
        log.info("Sorteo {} ejecutado: ganó el ticket {} de {}", id, winner, tickets.size)
        return true
    }

    /** El premio dejó de estar ratificado o aprobado: se anula y se devuelven los puntos. */
    private fun cancel(id: Long, params: MapSqlParameterSource) {
        jdbc.update(
            """
            INSERT INTO movimientos_puntos (cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion)
            SELECT t.cliente_id, CAST('REEMBOLSO_TICKET_SORTEO' AS tipo_movimiento_puntos_enum),
                   t.puntos_costo, 'tickets_sorteo', t.id, 'Reembolso: el sorteo fue anulado'
            FROM tickets_sorteo t
            WHERE t.sorteo_id = :id AND t.estado = 'VIGENTE'
            ORDER BY t.id
            """.trimIndent(),
            params,
        )
        val total = jdbc.update("UPDATE tickets_sorteo SET estado = 'ANULADO' WHERE sorteo_id = :id AND estado = 'VIGENTE'", params)
        params.addValue("total", total)
        jdbc.update("UPDATE sorteos SET estado = 'ANULADO', ejecutado_en = now(), total_tickets = :total WHERE id = :id", params)
        log.warn("Sorteo {} anulado, tickets reembolsados: {}", id, total)
    }
}