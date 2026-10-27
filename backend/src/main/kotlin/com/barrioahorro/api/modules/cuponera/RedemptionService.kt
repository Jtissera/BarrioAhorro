package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.ScanRedemptionResponse
import com.barrioahorro.api.modules.cuponera.dto.ScanResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

@Service
class RedemptionService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val commitments: CuponeraCommitmentRepository,
) {
    private val log = LoggerFactory.getLogger(RedemptionService::class.java)

    private data class RedemptionRow(
        val id: Long,
        val clientId: Long,
        val status: String,
        val commitmentStatus: String,
        val points: Int,
        val expiresAt: OffsetDateTime?,
        val validatedAt: OffsetDateTime?,
        val productName: String,
        val photoUrl: String?,
        val businessId: Long,
        val clientEmail: String,
    )

    /**
     * Valida un canje a partir del código del QR. Los rechazos de negocio se
     * devuelven con HTTP 200 y un [ScanResult]: así los efectos (p. ej. marcar
     * VENCIDO) se confirman y la app arma cada pantalla de resultado.
     */
    @Transactional
    fun validate(userId: Long, rawCode: String): ScanRedemptionResponse {
        commitments.findBusinessInfo(userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden validar canjes")

        val code = normalizeCode(rawCode) ?: return outcome(ScanResult.NOT_FOUND)

        // FOR UPDATE: dos escaneos simultáneos del mismo QR se serializan; el segundo ve VALIDADO.
        val row = jdbc.query(
            """
            SELECT cj.id, cj.cliente_id, cj.estado::text AS status, cj.puntos_costo,
                   cj.fecha_vencimiento, cj.fecha_validacion,
                   p.nombre_producto, p.foto_url, cc.comercio_id, cc.estado::text AS commitment_status, u.email
            FROM canjes_cupon cj
            JOIN cupones_cuponera p ON p.id = cj.cupon_id
            JOIN compromisos_cuponera cc ON cc.id = p.compromiso_id
            JOIN usuarios u ON u.id = cj.cliente_id
            WHERE cj.codigo_qr = :code
            FOR UPDATE OF cj
            """,
            MapSqlParameterSource("code", code),
        ) { rs, _ ->
            RedemptionRow(
                id = rs.getLong("id"),
                clientId = rs.getLong("cliente_id"),
                status = rs.getString("status"),
                commitmentStatus = rs.getString("commitment_status"),
                points = rs.getInt("puntos_costo"),
                expiresAt = rs.getObject("fecha_vencimiento", OffsetDateTime::class.java),
                validatedAt = rs.getObject("fecha_validacion", OffsetDateTime::class.java),
                productName = rs.getString("nombre_producto"),
                photoUrl = rs.getString("foto_url"),
                businessId = rs.getLong("comercio_id"),
                clientEmail = rs.getString("email"),
            )
        }.firstOrNull() ?: return outcome(ScanResult.NOT_FOUND)

        // Primero la pertenencia: a otro comercio no se le revela ningún dato.
        if (row.businessId != userId) {
            log.warn("Comercio {} escaneó un canje del comercio {}", userId, row.businessId)
            return outcome(ScanResult.OTHER_BUSINESS)
        }

        return when (row.status) {
            "VALIDADO" -> details(ScanResult.ALREADY_USED, row, row.validatedAt)
            "CANCELADO" -> details(ScanResult.CANCELLED, row, null)
            "VENCIDO" -> details(ScanResult.EXPIRED, row, null)
            else -> validatePending(row)
        }
    }

    private fun validatePending(row: RedemptionRow): ScanRedemptionResponse {
        val idParam = MapSqlParameterSource("id", row.id)

        if (row.commitmentStatus != "RATIFICADO") {
            jdbc.update(
                "UPDATE canjes_cupon SET estado = 'CANCELADO' WHERE id = :id AND estado = 'PENDIENTE_VALIDACION'",
                idParam,
            )
            return details(ScanResult.CANCELLED, row, null)
        }

        if (row.expiresAt != null && !OffsetDateTime.now().isBefore(row.expiresAt)) {
            jdbc.update(
                "UPDATE canjes_cupon SET estado = 'VENCIDO' WHERE id = :id AND estado = 'PENDIENTE_VALIDACION'",
                idParam,
            )
            return details(ScanResult.EXPIRED, row, null)
        }

        // Se bloquea el saldo del vecino para no reventar el CHECK de la base con un 500.
        val balance = jdbc.queryForObject(
            "SELECT saldo_puntos FROM perfiles_cliente WHERE usuario_id = :clientId FOR UPDATE",
            MapSqlParameterSource("clientId", row.clientId),
            Int::class.java,
        ) ?: 0
        if (balance < row.points) return details(ScanResult.INSUFFICIENT_POINTS, row, null)

        // El trigger de la base suma el stock canjeado y descuenta los puntos en esta misma transacción.
        val updated = jdbc.update(
            "UPDATE canjes_cupon SET estado = 'VALIDADO' WHERE id = :id AND estado = 'PENDIENTE_VALIDACION'",
            idParam,
        )
        if (updated != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El canje cambió mientras se validaba, escaneá de nuevo")
        }

        val validatedAt = jdbc.queryForObject(
            "SELECT fecha_validacion FROM canjes_cupon WHERE id = :id",
            idParam,
            OffsetDateTime::class.java,
        )
        return details(ScanResult.VALIDATED, row, validatedAt)
    }

    private fun normalizeCode(raw: String): String? =
        raw.trim().takeIf { UUID_REGEX.matches(it) }?.lowercase()

    private fun outcome(result: ScanResult) = ScanRedemptionResponse(result = result)

    private fun details(result: ScanResult, row: RedemptionRow, date: OffsetDateTime?) =
        ScanRedemptionResponse(
            result = result,
            receiptCode = "CJ-%06d".format(row.id),
            productName = row.productName,
            photoUrl = row.photoUrl,
            points = row.points,
            neighbor = maskEmail(row.clientEmail),
            date = date,
        )

    private fun maskEmail(email: String): String {
        val parts = email.split("@", limit = 2)
        if (parts.size != 2) return "***"
        return parts[0].take(2) + "***@" + parts[1]
    }
}