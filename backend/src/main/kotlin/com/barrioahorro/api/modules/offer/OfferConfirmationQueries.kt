package com.barrioahorro.api.modules.offer

import org.springframework.dao.EmptyResultDataAccessException
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

    fun upsertValidation(offerId: Long, clientId: Long, sigueVigente: Boolean = true) {
        val sql = """
            INSERT INTO validaciones_comunitarias_oferta (oferta_id, cliente_id, sigue_vigente)
            VALUES (?, ?, ?)
            ON CONFLICT (oferta_id, cliente_id)
            DO UPDATE SET sigue_vigente = EXCLUDED.sigue_vigente, updated_at = now()
        """.trimIndent()

        jdbc.update(sql, offerId, clientId, sigueVigente)
    }

    fun countValidations(offerId: Long): Long {
        val sql = """
            SELECT COUNT(*) 
            FROM validaciones_comunitarias_oferta 
            WHERE oferta_id = ? AND sigue_vigente = TRUE
        """.trimIndent()

        return jdbc.queryForObject(sql, Long::class.java, offerId) ?: 0L
    }

    fun isValidatedByClient(offerId: Long, clientId: Long): Boolean {
        val sql = """
            SELECT sigue_vigente 
            FROM validaciones_comunitarias_oferta 
            WHERE oferta_id = ? AND cliente_id = ?
        """.trimIndent()

        return try {
            jdbc.queryForObject(sql, Boolean::class.java, offerId, clientId) ?: false
        } catch (e: EmptyResultDataAccessException) {
            false
        }
    }

    fun isClientUser(userId: Long): Boolean {
        val sql = """
        SELECT COUNT(*) 
        FROM usuarios 
        WHERE id = ? 
          AND tipo_usuario::text = 'CLIENTE'
    """.trimIndent()
        val count = jdbc.queryForObject(sql, Long::class.java, userId) ?: 0L
        return count > 0L
    }
}