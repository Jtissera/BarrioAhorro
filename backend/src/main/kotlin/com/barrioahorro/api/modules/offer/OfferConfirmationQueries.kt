package com.barrioahorro.api.modules.offer

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class OfferConfirmationQueries(private val jdbc: JdbcTemplate) {

    /** Vecinos que confirmaron que la oferta sigue vigente, por oferta, para todas las ofertas del comercio. */
    fun countByOffer(businessId: Long): Map<Long, Int> =
        jdbc.query(
            """
            SELECT v.oferta_id, COUNT(*) AS confirmaciones
            FROM validaciones_comunitarias_oferta v
            JOIN ofertas o ON o.id = v.oferta_id
            WHERE o.comercio_id = ? AND v.sigue_vigente
            GROUP BY v.oferta_id
            """.trimIndent(),
            RowMapper { rs, _ -> rs.getLong("oferta_id") to rs.getInt("confirmaciones") },
            businessId,
        ).toMap()
}