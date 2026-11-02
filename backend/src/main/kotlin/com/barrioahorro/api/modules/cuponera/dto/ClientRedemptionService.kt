package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.RedemptionResponse
import com.barrioahorro.api.modules.explore.ExploreService
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class ClientRedemptionService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val explore: ExploreService,
) {

    private data class LockedCoupon(val cost: Int, val stock: Int, val available: Boolean)

    companion object {
        private const val PENDING = "PENDIENTE_VALIDACION"

        private const val SELECT_REDEMPTION = """
            SELECT cj.id, cj.codigo_qr, cj.estado::text AS estado, cj.puntos_costo, cj.fecha_canje,
                   cj.fecha_validacion, cc.periodo_fin,
                   p.nombre_producto, p.foto_url, pc.usuario_id AS comercio_id, pc.nombre_negocio
            FROM canjes_cupon cj
            JOIN cupones_cuponera p ON p.id = cj.cupon_id
            JOIN compromisos_cuponera cc ON cc.id = p.compromiso_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cc.comercio_id
        """
    }

    /**
     * Genera el QR. No toca los puntos: los descuenta la base cuando el comercio valida.
     * El lock sobre el cupón serializa los canjes de ese cupón (stock y "uno por vecino" sin carreras).
     */
    @Transactional
    fun redeem(userId: Long, couponId: Long): RedemptionResponse {
        explore.requireClient(userId)

        val coupon = jdbc.query(
            """
            SELECT cu.costo_puntos, cu.stock_comprometido,
                   ((${CouponCatalogService.COUPON_IS_ACTIVE}) AND (${ExploreService.BUSINESS_IS_VISIBLE})) AS disponible
            FROM cupones_cuponera cu
            JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cc.comercio_id
            WHERE cu.id = :id
            FOR UPDATE OF cu
            """.trimIndent(),
            MapSqlParameterSource("id", couponId),
        ) { rs, _ ->
            LockedCoupon(rs.getInt("costo_puntos"), rs.getInt("stock_comprometido"), rs.getBoolean("disponible"))
        }.firstOrNull()

        if (coupon == null || !coupon.available) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Este cupón ya no está disponible")
        }

        val taken = jdbc.queryForObject(
            "SELECT COUNT(*) FROM canjes_cupon WHERE cupon_id = :id AND estado IN ('PENDIENTE_VALIDACION', 'VALIDADO')",
            MapSqlParameterSource("id", couponId),
            Long::class.java,
        ) ?: 0L
        if (taken >= coupon.stock) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Se agotaron las unidades de este cupón")
        }

        val alreadyPending = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM canjes_cupon WHERE cupon_id = :c AND cliente_id = :u AND estado = 'PENDIENTE_VALIDACION')",
            MapSqlParameterSource().addValue("c", couponId).addValue("u", userId),
            Boolean::class.java,
        ) ?: false
        if (alreadyPending) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya canjeaste este cupón: tu código está en Mis canjes")
        }

        val balance = jdbc.queryForObject(
            "SELECT saldo_puntos FROM perfiles_cliente WHERE usuario_id = :u",
            MapSqlParameterSource("u", userId),
            Int::class.java,
        ) ?: 0
        if (balance < coupon.cost) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No te alcanzan los puntos para este cupón")
        }

        // El trigger de la base pisa puntos_costo con el valor vigente; se manda igual por el NOT NULL.
        val id = jdbc.queryForObject(
            """
            INSERT INTO canjes_cupon (cupon_id, cliente_id, puntos_costo, fecha_vencimiento)
            SELECT cu.id, :userId, cu.costo_puntos,
                   ((cc.periodo_fin + 1)::timestamp AT TIME ZONE 'America/Argentina/Buenos_Aires')
            FROM cupones_cuponera cu
            JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
            WHERE cu.id = :couponId
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource().addValue("userId", userId).addValue("couponId", couponId),
            Long::class.java,
        ) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el canje")

        return load(userId, id)
    }

    @Transactional
    fun list(userId: Long): List<RedemptionResponse> {
        explore.requireClient(userId)
        sweep(userId)
        return jdbc.query(
            "$SELECT_REDEMPTION WHERE cj.cliente_id = :userId ORDER BY cj.fecha_canje DESC, cj.id DESC LIMIT 100",
            MapSqlParameterSource("userId", userId),
        ) { rs, _ -> toResponse(rs) }
    }

    @Transactional
    fun detail(userId: Long, id: Long): RedemptionResponse {
        explore.requireClient(userId)
        sweep(userId)
        return load(userId, id)
    }

    /** Solo se cancela un canje todavía pendiente: no hay puntos que devolver. */
    @Transactional
    fun cancel(userId: Long, id: Long): RedemptionResponse {
        explore.requireClient(userId)
        val updated = jdbc.update(
            "UPDATE canjes_cupon SET estado = 'CANCELADO' WHERE id = :id AND cliente_id = :u AND estado = '$PENDING'",
            MapSqlParameterSource().addValue("id", id).addValue("u", userId),
        )
        if (updated != 1) {
            // Si el comercio lo validó justo antes, el UPDATE no encuentra la fila pendiente.
            load(userId, id)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Este canje ya no se puede cancelar")
        }
        return load(userId, id)
    }

    /**
     * Deja los canjes pendientes del vecino al día: vence los que pasaron su fecha y cancela los
     * de un compromiso que ya no está ratificado (lo mismo que hace el escáner del comercio).
     */
    private fun sweep(userId: Long) {
        val params = MapSqlParameterSource("userId", userId)
        jdbc.update(
            """
            UPDATE canjes_cupon SET estado = 'VENCIDO'
            WHERE cliente_id = :userId AND estado = 'PENDIENTE_VALIDACION' AND fecha_vencimiento <= now()
            """.trimIndent(),
            params,
        )
        jdbc.update(
            """
            UPDATE canjes_cupon cj SET estado = 'CANCELADO'
            FROM cupones_cuponera p JOIN compromisos_cuponera cc ON cc.id = p.compromiso_id
            WHERE p.id = cj.cupon_id AND cj.cliente_id = :userId
              AND cj.estado = 'PENDIENTE_VALIDACION' AND cc.estado <> 'RATIFICADO'
            """.trimIndent(),
            params,
        )
    }

    private fun load(userId: Long, id: Long): RedemptionResponse =
        jdbc.query(
            "$SELECT_REDEMPTION WHERE cj.id = :id AND cj.cliente_id = :userId",
            MapSqlParameterSource().addValue("id", id).addValue("userId", userId),
        ) { rs, _ -> toResponse(rs) }.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos este canje")

    private fun toResponse(rs: java.sql.ResultSet): RedemptionResponse {
        val status = rs.getString("estado")
        val id = rs.getLong("id")
        return RedemptionResponse(
            id = id,
            receiptCode = "CJ-%06d".format(id),
            code = rs.getString("codigo_qr").takeIf { status == PENDING },
            status = status,
            productName = rs.getString("nombre_producto"),
            photoUrl = rs.getString("foto_url")?.takeIf { it.isNotBlank() },
            businessId = rs.getLong("comercio_id"),
            businessName = rs.getString("nombre_negocio"),
            pointsCost = rs.getInt("puntos_costo"),
            redeemedAt = rs.getObject("fecha_canje", OffsetDateTime::class.java),
            validatedAt = rs.getObject("fecha_validacion", OffsetDateTime::class.java),
            validUntil = rs.getObject("periodo_fin", LocalDate::class.java),
        )
    }
}