package com.barrioahorro.api.modules.business

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.sql.ResultSet

data class TopOfferRow(val id: Long, val title: String, val views: Int)

data class ReviewRow(val id: Long, val authorEmail: String, val rating: Int, val comment: String)

data class ActiveOfferRow(
    val id: Long,
    val title: String,
    val vigenciaTipo: String,
    val vigenteHasta: OffsetDateTime?,
    val unitPrice: BigDecimal?,
    val tipo: String,
    val porcentaje: Int?,
    val llevar: Int?,
    val pagar: Int?,
    val cantidadRequerida: Int?,
    val descuentoUltima: Int?,
)

private fun ResultSet.intOrNull(column: String): Int? {
    val value = getInt(column)
    return if (wasNull()) null else value
}

/**
 *
 * Usa JdbcTemplate para no depender de entidades de ofertas/reseñas/visualizaciones.
 */
@Repository
class DashboardQueries(private val jdbc: JdbcTemplate) {

    /** Visualizaciones del perfil del comercio por día local en [from, to). */
    fun viewsByDay(comercioId: Long, from: OffsetDateTime, to: OffsetDateTime, zone: String): Map<LocalDate, Int> =
        jdbc.query(
            """
            SELECT (created_at AT TIME ZONE ?)::date AS dia, COUNT(*) AS vistas
            FROM visualizaciones_comercio
            WHERE comercio_id = ? AND created_at >= ? AND created_at < ?
            GROUP BY dia
            """.trimIndent(),
            RowMapper { rs, _ -> rs.getObject("dia", LocalDate::class.java) to rs.getInt("vistas") },
            zone, comercioId, from, to,
        ).toMap()

    fun topOffers(comercioId: Long, from: OffsetDateTime, to: OffsetDateTime, limit: Int): List<TopOfferRow> =
        jdbc.query(
            """
            SELECT o.id, o.nombre_producto, COUNT(v.id) AS vistas
            FROM ofertas o
            JOIN visualizaciones_oferta v ON v.oferta_id = o.id
            WHERE o.comercio_id = ? AND v.created_at >= ? AND v.created_at < ?
            GROUP BY o.id, o.nombre_producto
            ORDER BY vistas DESC, o.id
            LIMIT ?
            """.trimIndent(),
            RowMapper { rs, _ -> TopOfferRow(rs.getLong("id"), rs.getString("nombre_producto"), rs.getInt("vistas")) },
            comercioId, from, to, limit,
        )

    fun recentReviews(comercioId: Long, limit: Int): List<ReviewRow> =
        jdbc.query(
            """
            SELECT r.id, u.email, r.estrellas, r.texto
            FROM resenas_comercio r
            JOIN usuarios u ON u.id = r.cliente_id
            WHERE r.comercio_id = ?
            ORDER BY r.created_at DESC
            LIMIT ?
            """.trimIndent(),
            RowMapper { rs, _ ->
                ReviewRow(rs.getLong("id"), rs.getString("email"), rs.getInt("estrellas"), rs.getString("texto"))
            },
            comercioId, limit,
        )

    /** Ofertas vigentes, las más recientes primero. */
    fun activeOffers(comercioId: Long, limit: Int): List<ActiveOfferRow> =
        jdbc.query(
            """
            SELECT o.id, o.nombre_producto, o.vigencia_tipo::text AS vigencia_tipo, o.vigente_hasta,
                   o.tipo_oferta::text AS tipo,
                   COALESCE(op.precio_final, d.precio_unitario, oc.precio_unitario, om.precio_unitario_mayorista) AS precio,
                   op.porcentaje_descuento AS porcentaje,
                   d.unidades_a_llevar AS llevar,
                   d.unidades_a_pagar AS pagar,
                   oc.cantidad_requerida AS cantidad_requerida,
                   oc.porcentaje_descuento_ultima_unidad AS descuento_ultima
            FROM ofertas o
            LEFT JOIN ofertas_porcentaje op ON op.oferta_id = o.id
            LEFT JOIN ofertas_2x1 d ON d.oferta_id = o.id
            LEFT JOIN ofertas_cantidad oc ON oc.oferta_id = o.id
            LEFT JOIN ofertas_por_mayor om ON om.oferta_id = o.id
            WHERE o.comercio_id = ? AND o.activa AND (o.vigente_hasta IS NULL OR o.vigente_hasta > now())
            ORDER BY o.created_at DESC
            LIMIT ?
            """.trimIndent(),
            RowMapper { rs, _ ->
                ActiveOfferRow(
                    id = rs.getLong("id"),
                    title = rs.getString("nombre_producto"),
                    vigenciaTipo = rs.getString("vigencia_tipo"),
                    vigenteHasta = rs.getObject("vigente_hasta", OffsetDateTime::class.java),
                    unitPrice = rs.getBigDecimal("precio"),
                    tipo = rs.getString("tipo"),
                    porcentaje = rs.intOrNull("porcentaje"),
                    llevar = rs.intOrNull("llevar"),
                    pagar = rs.intOrNull("pagar"),
                    cantidadRequerida = rs.intOrNull("cantidad_requerida"),
                    descuentoUltima = rs.intOrNull("descuento_ultima"),
                )
            },
            comercioId, limit,
        )
}