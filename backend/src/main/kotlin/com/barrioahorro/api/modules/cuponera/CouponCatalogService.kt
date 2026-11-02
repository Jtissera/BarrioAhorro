package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CouponResponse
import com.barrioahorro.api.modules.cuponera.dto.CouponsPage
import com.barrioahorro.api.modules.explore.ExploreService
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class CouponFilters(
    val latitude: Double?,
    val longitude: Double?,
    val radiusKm: Double?,
    val categoryIds: List<Int>,
    val query: String?,
)

enum class CouponOrder { DISTANCE, POINTS, RECENT }

@Service
class CouponCatalogService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val explore: ExploreService,
) {

    companion object {
        const val MAX_RADIUS_KM = 20.0
        const val MAX_PAGE_SIZE = 50

        /** Compromiso ratificado que rige hoy y premio aprobado por el admin. */
        const val COUPON_IS_ACTIVE = """
            cc.estado = 'RATIFICADO'
            AND (now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
                BETWEEN cc.periodo_inicio AND cc.periodo_fin
            AND cu.estado_revision = 'APROBADO'
        """

        /** Unidades tomadas: validadas + QR pendientes (el mismo criterio que el trigger de la base). */
        const val UNITS_TAKEN = """(SELECT COUNT(*) FROM canjes_cupon x
            WHERE x.cupon_id = cu.id AND x.estado IN ('PENDIENTE_VALIDACION', 'VALIDADO'))"""

        private const val FROM = """
            cupones_cuponera cu
            JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
            JOIN perfiles_comercio pc ON pc.usuario_id = cc.comercio_id
            LEFT JOIN rubros r ON r.id = pc.rubro_id
        """

        private const val POINT_EXPR = "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)"
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, f: CouponFilters, order: CouponOrder, page: Int, size: Int): CouponsPage {
        explore.requireClient(userId)

        val hasLocation = f.latitude != null && f.longitude != null
        val params = MapSqlParameterSource()
        if (hasLocation) {
            params.addValue("lat", f.latitude).addValue("lng", f.longitude)
        }
        val where = where(f, hasLocation, params)
        val distance = if (hasLocation) {
            "ST_Distance(pc.ubicacion, $POINT_EXPR)"
        } else {
            "CAST(NULL AS double precision)"
        }

        val effectiveOrder = if (order == CouponOrder.DISTANCE && !hasLocation) CouponOrder.RECENT else order
        val orderBy = when (effectiveOrder) {
            CouponOrder.DISTANCE -> "distancia_m ASC NULLS LAST, cu.id"
            CouponOrder.POINTS -> "cu.costo_puntos ASC, cu.id"
            CouponOrder.RECENT -> "cu.id DESC"
        }

        val total = jdbc.queryForObject("SELECT COUNT(*) FROM $FROM WHERE $where", params, Long::class.java) ?: 0L

        params.addValue("limit", size).addValue("offset", page.toLong() * size)
        val items = jdbc.query(
            """
            SELECT cu.id, cu.nombre_producto, cu.descripcion, cu.foto_url, cu.costo_puntos,
                   (cu.stock_comprometido - $UNITS_TAKEN) AS disponibles,
                   cc.periodo_fin,
                   pc.usuario_id AS comercio_id, pc.nombre_negocio, r.nombre AS rubro,
                   $distance AS distancia_m
            FROM $FROM
            WHERE $where
            ORDER BY $orderBy
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params,
        ) { rs, _ ->
            val dist = rs.getDouble("distancia_m").let { if (rs.wasNull()) null else it.toInt() }
            CouponResponse(
                id = rs.getLong("id"),
                productName = rs.getString("nombre_producto"),
                description = rs.getString("descripcion")?.takeIf { it.isNotBlank() },
                photoUrl = rs.getString("foto_url")?.takeIf { it.isNotBlank() },
                pointsCost = rs.getInt("costo_puntos"),
                availableUnits = rs.getInt("disponibles"),
                validUntil = rs.getObject("periodo_fin", LocalDate::class.java),
                businessId = rs.getLong("comercio_id"),
                businessName = rs.getString("nombre_negocio"),
                category = rs.getString("rubro"),
                distanceMeters = dist,
            )
        }

        return CouponsPage(
            items = items,
            total = total,
            page = page,
            size = size,
            hasMore = (page.toLong() + 1) * size < total,
        )
    }

    private fun where(f: CouponFilters, hasLocation: Boolean, params: MapSqlParameterSource): String {
        val clauses = mutableListOf(
            COUPON_IS_ACTIVE,
            "$UNITS_TAKEN < cu.stock_comprometido",
            ExploreService.BUSINESS_IS_VISIBLE,
        )

        // El radio solo filtra si el vecino lo pidió; sin radio se muestran todos y se ordena por cercanía.
        if (hasLocation && f.radiusKm != null) {
            params.addValue("radius", minOf(f.radiusKm, MAX_RADIUS_KM) * 1000.0)
            clauses += "ST_DWithin(pc.ubicacion, $POINT_EXPR, :radius)"
        }
        if (f.categoryIds.isNotEmpty()) {
            params.addValue("categoryIds", f.categoryIds)
            clauses += "pc.rubro_id IN (:categoryIds)"
        }
        if (f.query != null) {
            params.addValue("q", likePattern(f.query))
            clauses += "(pc.nombre_negocio ILIKE :q ESCAPE '\\' OR cu.nombre_producto ILIKE :q ESCAPE '\\')"
        }
        return clauses.joinToString(" AND ") { "($it)" }
    }

    private fun likePattern(raw: String): String {
        val escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}