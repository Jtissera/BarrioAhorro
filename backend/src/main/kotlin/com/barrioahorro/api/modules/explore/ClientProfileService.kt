package com.barrioahorro.api.modules.explore

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@Service
class ClientProfileService(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    companion object {
        const val MAX_PAGE_SIZE = 50
    }

    /** Las reseñas de comercios que ya no son visibles no se cuentan ni se listan: no se podrían abrir. */
    private val visible = ExploreService.BUSINESS_IS_VISIBLE.trimIndent()

    @Transactional(readOnly = true)
    fun profile(userId: Long): ClientProfileResponse =
        jdbc.query(
            """
            SELECT u.email, c.saldo_puntos,
                   (SELECT COUNT(*) FROM foro_publicaciones p
                     WHERE p.autor_id = u.id AND p.eliminado_en IS NULL) AS publicaciones,
                   (SELECT COUNT(*) FROM resenas_comercio r
                     JOIN perfiles_comercio pc ON pc.usuario_id = r.comercio_id
                     WHERE r.cliente_id = u.id AND ($visible)) AS resenas
            FROM usuarios u
            JOIN perfiles_cliente c ON c.usuario_id = u.id
            WHERE u.id = :id
            """.trimIndent(),
            MapSqlParameterSource("id", userId),
        ) { rs, _ ->
            ClientProfileResponse(
                name = rs.getString("email").substringBefore('@'),
                points = rs.getInt("saldo_puntos"),
                postsCount = rs.getInt("publicaciones"),
                reviewsCount = rs.getInt("resenas"),
            )
        }.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los vecinos tienen perfil de vecino")

    @Transactional(readOnly = true)
    fun myReviews(userId: Long, page: Int, size: Int): MyReviewsPage {
        if (page < 0) bad("La página no es válida")
        if (size !in 1..MAX_PAGE_SIZE) bad("El tamaño de página debe estar entre 1 y $MAX_PAGE_SIZE")

        val from = """
            resenas_comercio r
            JOIN perfiles_comercio pc ON pc.usuario_id = r.comercio_id
            LEFT JOIN rubros rb ON rb.id = pc.rubro_id
            WHERE r.cliente_id = :c AND ($visible)
        """.trimIndent()

        val params = MapSqlParameterSource("c", userId)
        val total = jdbc.queryForObject("SELECT COUNT(*) FROM $from", params, Long::class.java) ?: 0L

        params.addValue("limit", size).addValue("offset", page.toLong() * size)
        val items = jdbc.query(
            """
            SELECT r.id, r.comercio_id, pc.nombre_negocio, rb.nombre AS rubro,
                   r.estrellas, r.texto, r.created_at
            FROM $from
            ORDER BY r.created_at DESC, r.id DESC
            LIMIT :limit OFFSET :offset
            """,
            params,
        ) { rs, _ ->
            MyReviewResponse(
                id = rs.getLong("id"),
                businessId = rs.getLong("comercio_id"),
                businessName = rs.getString("nombre_negocio"),
                category = rs.getString("rubro"),
                rating = rs.getInt("estrellas"),
                comment = rs.getString("texto"),
                createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            )
        }

        return MyReviewsPage(
            items = items,
            total = total,
            page = page,
            size = size,
            hasNext = (page.toLong() + 1) * size < total,
        )
    }

    private fun bad(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
}