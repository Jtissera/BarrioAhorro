package com.barrioahorro.api.modules.business

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

data class BusinessReviewRow(
    val id: Long,
    val authorEmail: String,
    val rating: Int,
    val comment: String,
    val createdAt: OffsetDateTime,
)

@Repository
class BusinessReviewQueries(private val jdbc: JdbcTemplate) {

    /** Cantidad de reseñas por cantidad de estrellas. Solo incluye las estrellas que tienen reseñas. */
    fun countByStars(businessId: Long): Map<Int, Int> =
        jdbc.query(
            """
            SELECT estrellas, COUNT(*) AS cantidad
            FROM resenas_comercio
            WHERE comercio_id = ?
            GROUP BY estrellas
            """.trimIndent(),
            RowMapper { rs, _ -> rs.getInt("estrellas") to rs.getInt("cantidad") },
            businessId,
        ).toMap()

    /** Reseñas más recientes primero. El id desempata para que la paginación sea estable. */
    fun findPage(businessId: Long, limit: Int, offset: Long): List<BusinessReviewRow> =
        jdbc.query(
            """
            SELECT r.id, u.email, r.estrellas, r.texto, r.created_at
            FROM resenas_comercio r
            JOIN usuarios u ON u.id = r.cliente_id
            WHERE r.comercio_id = ?
            ORDER BY r.created_at DESC, r.id DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            RowMapper { rs, _ ->
                BusinessReviewRow(
                    id = rs.getLong("id"),
                    authorEmail = rs.getString("email"),
                    rating = rs.getInt("estrellas"),
                    comment = rs.getString("texto"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
                )
            },
            businessId, limit, offset,
        )
}