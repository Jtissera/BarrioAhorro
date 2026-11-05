package com.barrioahorro.api.modules.sorteos

import com.barrioahorro.api.modules.explore.ExploreService
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PrizeService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val explore: ExploreService,
) {

    companion object {
        const val QR_PREFIX = "BA-SORTEO:"

        private const val STATUS_SQL = """
            CASE WHEN p.canjeado_en IS NOT NULL THEN 'CANJEADO'
                 WHEN p.vence_en <= now() THEN 'VENCIDO'
                 ELSE 'CANJEABLE' END
        """
    }

    private fun ResultSet.prizeName(): String =
        getString("premio_nombre")?.takeIf { it.isNotBlank() } ?: getString("premio_descripcion").orEmpty()

    // ---- Vecino ---------------------------------------------------------------------

    /** Premios ganados que el vecino todavía no vio. */
    @Transactional(readOnly = true)
    fun pendingNotices(userId: Long): List<WinnerNoticeResponse> {
        explore.requireClient(userId)
        return jdbc.query(
            """
            SELECT p.ticket_id, cs.premio_nombre, cs.premio_descripcion, cs.premio_foto_url, pc.nombre_negocio
            FROM premios_sorteo p
            JOIN sorteos s ON s.id = p.sorteo_id
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cs.comercio_id
            WHERE p.cliente_id = :u AND p.visto_en IS NULL
            ORDER BY p.created_at, p.ticket_id
            """.trimIndent(),
            MapSqlParameterSource("u", userId),
        ) { rs, _ ->
            val business = rs.getString("nombre_negocio")
            WinnerNoticeResponse(
                ticketId = rs.getLong("ticket_id"),
                raffleTitle = if (business.isNullOrBlank()) "Sorteo del barrio" else "Sorteo de $business",
                prizeName = rs.prizeName(),
                prizePhotoUrl = rs.getString("premio_foto_url")?.takeIf { it.isNotBlank() },
                businessName = business,
            )
        }
    }

    @Transactional
    fun markNoticeSeen(userId: Long, ticketId: Long) {
        explore.requireClient(userId)
        jdbc.update(
            "UPDATE premios_sorteo SET visto_en = now() WHERE ticket_id = :t AND cliente_id = :u AND visto_en IS NULL",
            MapSqlParameterSource().addValue("t", ticketId).addValue("u", userId),
        )
    }

    @Transactional(readOnly = true)
    fun detail(userId: Long, ticketId: Long): PrizeDetailResponse {
        explore.requireClient(userId)
        return jdbc.query(
            """
            SELECT p.ticket_id, p.token, p.vence_en, p.canjeado_en, $STATUS_SQL AS estado,
                   cs.premio_nombre, cs.premio_descripcion, cs.premio_foto_url, pc.nombre_negocio
            FROM premios_sorteo p
            JOIN tickets_sorteo t ON t.id = p.ticket_id AND t.estado = 'GANADOR'
            JOIN sorteos s ON s.id = p.sorteo_id
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cs.comercio_id
            WHERE p.ticket_id = :t AND p.cliente_id = :u
            """.trimIndent(),
            MapSqlParameterSource().addValue("t", ticketId).addValue("u", userId),
        ) { rs, _ ->
            val status = rs.getString("estado")
            PrizeDetailResponse(
                ticketId = rs.getLong("ticket_id"),
                ticketNumber = rs.getLong("ticket_id"),
                prizeName = rs.prizeName(),
                prizeDescription = rs.getString("premio_descripcion")?.takeIf { it.isNotBlank() },
                prizePhotoUrl = rs.getString("premio_foto_url")?.takeIf { it.isNotBlank() },
                businessName = rs.getString("nombre_negocio"),
                status = status,
                expiresAt = rs.getObject("vence_en", OffsetDateTime::class.java),
                redeemedAt = rs.getObject("canjeado_en", OffsetDateTime::class.java),
                qrPayload = if (status == "CANJEABLE") QR_PREFIX + rs.getString("token") else null,
            )
        }.firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese premio")
    }

    // ---- Comerciante ----------------------------------------------------------------

    private fun requireMerchant(userId: Long) {
        val ok = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM perfiles_comercio WHERE usuario_id = :id)",
            MapSqlParameterSource("id", userId),
            Boolean::class.java,
        ) ?: false
        if (!ok) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden validar premios")
    }

    private fun parseToken(code: String): UUID? =
        runCatching { UUID.fromString(code.trim().removePrefix(QR_PREFIX)) }.getOrNull()

    private fun invalid(reason: String, message: String) = PrizeScanResult(false, reason, message)

    /** Solo verifica: no cambia nada. */
    @Transactional(readOnly = true)
    fun verify(merchantId: Long, code: String): PrizeScanResult {
        requireMerchant(merchantId)
        val token = parseToken(code)
            ?: return invalid("INVALID_CODE", "Este código no corresponde a un premio de sorteo.")

        val row = jdbc.query(
            """
            SELECT p.ticket_id, p.vence_en, p.canjeado_en IS NOT NULL AS canjeado, p.vence_en <= now() AS vencido,
                   t.estado AS ticket_estado, cs.comercio_id,
                   cs.premio_nombre, cs.premio_descripcion, cs.premio_foto_url, u.email
            FROM premios_sorteo p
            JOIN tickets_sorteo t ON t.id = p.ticket_id
            JOIN sorteos s ON s.id = p.sorteo_id
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            JOIN usuarios u ON u.id = p.cliente_id
            WHERE p.token = :token
            """.trimIndent(),
            MapSqlParameterSource("token", token),
        ) { rs, _ ->
            val base = PrizeScanResult(
                valid = true,
                reason = null,
                message = "Ticket ganador válido.",
                prizeName = rs.prizeName(),
                prizeDescription = rs.getString("premio_descripcion")?.takeIf { it.isNotBlank() },
                prizePhotoUrl = rs.getString("premio_foto_url")?.takeIf { it.isNotBlank() },
                ticketNumber = rs.getLong("ticket_id"),
                winnerName = rs.getString("email").substringBefore('@'),
                expiresAt = rs.getObject("vence_en", OffsetDateTime::class.java),
            )
            when {
                rs.getString("ticket_estado") != "GANADOR" ->
                    base.copy(valid = false, reason = "NOT_WINNER", message = "Este ticket no es ganador.")
                rs.getLong("comercio_id") != merchantId ->
                    base.copy(valid = false, reason = "OTHER_BUSINESS", message = "Este premio corresponde a otro comercio.")
                rs.getBoolean("canjeado") ->
                    base.copy(valid = false, reason = "ALREADY_REDEEMED", message = "Este premio ya fue canjeado.")
                rs.getBoolean("vencido") ->
                    base.copy(valid = false, reason = "EXPIRED", message = "El código venció sin ser canjeado.")
                else -> base
            }
        }.firstOrNull()

        return row ?: invalid("INVALID_CODE", "No encontramos este premio.")
    }

    /** Confirma la entrega. El UPDATE condicional garantiza un solo canje aunque lleguen dos a la vez. */
    @Transactional
    fun redeem(merchantId: Long, code: String): PrizeScanResult {
        val preview = verify(merchantId, code)
        if (!preview.valid) return preview

        val updated = jdbc.update(
            """
            UPDATE premios_sorteo p
            SET canjeado_en = now(), canjeado_por = :m
            FROM sorteos s
            JOIN compromisos_sorteo cs ON cs.id = s.compromiso_sorteo_id
            WHERE s.id = p.sorteo_id
              AND p.token = :token
              AND cs.comercio_id = :m
              AND p.canjeado_en IS NULL
              AND p.vence_en > now()
              AND EXISTS (SELECT 1 FROM tickets_sorteo t WHERE t.id = p.ticket_id AND t.estado = 'GANADOR')
            """.trimIndent(),
            MapSqlParameterSource().addValue("m", merchantId).addValue("token", parseToken(code)),
        )
        // Si otro canje se adelantó, se informa el motivo real.
        if (updated == 0) return verify(merchantId, code)
        return preview.copy(message = "Premio entregado.")
    }
}