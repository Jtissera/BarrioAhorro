package com.barrioahorro.api.modules.explore

import com.barrioahorro.api.modules.business.BusinessReviewQueries
import com.barrioahorro.api.modules.business.BusinessScheduleRepository
import com.barrioahorro.api.modules.business.dto.BusinessReviewResponse
import com.barrioahorro.api.modules.business.dto.ReviewStarCount
import com.barrioahorro.api.modules.business.dto.ReviewsSummaryResponse
import com.barrioahorro.api.modules.business.dto.ScheduleSlotResponse
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Service
class BusinessProfileService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val explore: ExploreService,
    private val schedules: BusinessScheduleRepository,
    private val reviewQueries: BusinessReviewQueries,
) {

    companion object {
        const val MAX_PAGE_SIZE = 50
        const val MIN_COMMENT = 10
        const val MAX_COMMENT = 500
        private const val MAX_STARS = 5
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
        private const val POINT_EXPR = "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)"

        private const val REVIEW_SELECT = """
            SELECT r.id, u.email, r.estrellas, r.texto, r.created_at
            FROM resenas_comercio r
            JOIN usuarios u ON u.id = r.cliente_id
        """
    }

    private val reviewMapper = RowMapper<BusinessReviewResponse> { rs, _ ->
        BusinessReviewResponse(
            id = rs.getLong("id"),
            authorName = rs.getString("email").substringBefore('@'),
            rating = rs.getInt("estrellas"),
            comment = rs.getString("texto"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        )
    }

    // ---- Perfil y ofertas -----------------------------------------------------------

    @Transactional(readOnly = true)
    fun profile(userId: Long, businessId: Long, lat: Double?, lng: Double?): BusinessProfileResponse {
        explore.requireClient(userId)
        explore.requireVisibleBusiness(businessId)

        val params = MapSqlParameterSource("id", businessId)
        val distance = if (lat != null && lng != null) {
            params.addValue("lat", lat).addValue("lng", lng)
            "ST_Distance(pc.ubicacion, $POINT_EXPR)"
        } else {
            "CAST(NULL AS double precision)"
        }

        val base = jdbc.query(
            """
            SELECT pc.usuario_id, pc.nombre_negocio, r.nombre AS rubro, pc.direccion, pc.descripcion,
                   ST_Y(CAST(pc.ubicacion AS geometry)) AS lat,
                   ST_X(CAST(pc.ubicacion AS geometry)) AS lng,
                   $distance AS distancia_m,
                   (SELECT ph.url FROM fotos_comercio ph WHERE ph.comercio_id = pc.usuario_id
                     ORDER BY ph.orden, ph.id LIMIT 1) AS foto
            FROM perfiles_comercio pc
            LEFT JOIN rubros r ON r.id = pc.rubro_id
            WHERE pc.usuario_id = :id
            """.trimIndent(),
            params,
        ) { rs, _ ->
            val meters = rs.getDouble("distancia_m").let { if (rs.wasNull()) null else it.toInt() }
            BusinessProfileResponse(
                id = rs.getLong("usuario_id"),
                name = rs.getString("nombre_negocio"),
                category = rs.getString("rubro"),
                address = rs.getString("direccion"),
                description = rs.getString("descripcion"),
                latitude = rs.getDouble("lat"),
                longitude = rs.getDouble("lng"),
                distanceMeters = meters,
                photoUrl = rs.getString("foto"),
                schedule = emptyList(),
            )
        }.firstOrNull() ?: throw notFound()

        val slots = schedules.findByComercioIdOrderByDiaSemanaAscHoraInicioAsc(businessId)
            .filter { it.activo }
            .map {
                ScheduleSlotResponse(
                    diaSemana = it.diaSemana,
                    horaInicio = it.horaInicio.format(TIME_FORMAT),
                    horaFin = it.horaFin.format(TIME_FORMAT),
                    activo = it.activo,
                )
            }
        return base.copy(schedule = slots)
    }

    @Transactional(readOnly = true)
    fun offers(userId: Long, businessId: Long, lat: Double?, lng: Double?): List<ExploreOfferResponse> =
        explore.businessOffers(userId, businessId, lat, lng)

    // ---- Reseñas --------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun reviews(userId: Long, businessId: Long, page: Int, size: Int): BusinessReviewsPage {
        if (page < 0) bad("La página no es válida")
        if (size !in 1..MAX_PAGE_SIZE) bad("El tamaño de página debe estar entre 1 y $MAX_PAGE_SIZE")
        explore.requireClient(userId)
        explore.requireVisibleBusiness(businessId)

        val params = MapSqlParameterSource().addValue("b", businessId).addValue("c", userId)

        val mine = jdbc.query(
            "$REVIEW_SELECT WHERE r.comercio_id = :b AND r.cliente_id = :c",
            params,
            reviewMapper,
        ).firstOrNull()

        params.addValue("limit", size + 1).addValue("offset", page.toLong() * size)
        val rows = jdbc.query(
            """
            $REVIEW_SELECT
            WHERE r.comercio_id = :b AND r.cliente_id <> :c
            ORDER BY r.created_at DESC, r.id DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params,
            reviewMapper,
        )

        return BusinessReviewsPage(
            summary = buildSummary(reviewQueries.countByStars(businessId)),
            mine = mine,
            items = rows.take(size),
            page = page,
            size = size,
            hasNext = rows.size > size,
        )
    }

    @Transactional
    fun saveReview(userId: Long, businessId: Long, request: WriteReviewRequest): BusinessReviewResponse {
        explore.requireClient(userId)
        explore.requireVisibleBusiness(businessId)

        val rating = request.rating ?: bad("Elegí cuántas estrellas le das al comercio")
        val comment = request.comment.orEmpty().trim()
        if (comment.length !in MIN_COMMENT..MAX_COMMENT) {
            bad("La reseña debe tener entre $MIN_COMMENT y $MAX_COMMENT caracteres")
        }

        val params = MapSqlParameterSource()
            .addValue("b", businessId)
            .addValue("c", userId)
            .addValue("t", comment)
            .addValue("s", rating)

        return jdbc.queryForObject(
            """
            WITH saved AS (
                INSERT INTO resenas_comercio (comercio_id, cliente_id, texto, estrellas)
                VALUES (:b, :c, :t, :s)
                ON CONFLICT (comercio_id, cliente_id)
                DO UPDATE SET texto = EXCLUDED.texto, estrellas = EXCLUDED.estrellas
                RETURNING id, cliente_id, estrellas, texto, created_at
            )
            SELECT s.id, u.email, s.estrellas, s.texto, s.created_at
            FROM saved s
            JOIN usuarios u ON u.id = s.cliente_id
            """.trimIndent(),
            params,
            reviewMapper,
        ) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No pudimos guardar tu reseña")
    }

    @Transactional
    fun deleteReview(userId: Long, businessId: Long) {
        explore.requireClient(userId)
        val deleted = jdbc.update(
            "DELETE FROM resenas_comercio WHERE comercio_id = :b AND cliente_id = :c",
            MapSqlParameterSource().addValue("b", businessId).addValue("c", userId),
        )
        if (deleted == 0) throw ResponseStatusException(HttpStatus.NOT_FOUND, "No tenés una reseña en este comercio")
    }

    // ---- Helpers --------------------------------------------------------------------

    private fun buildSummary(countsByStars: Map<Int, Int>): ReviewsSummaryResponse {
        val total = countsByStars.values.sum()
        val average = if (total == 0) {
            0.0
        } else {
            round(countsByStars.entries.sumOf { (stars, count) -> stars * count } * 10.0 / total) / 10.0
        }
        return ReviewsSummaryResponse(
            average = average,
            total = total,
            distribution = (MAX_STARS downTo 1).map { ReviewStarCount(stars = it, count = countsByStars[it] ?: 0) },
        )
    }

    private fun bad(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    private fun notFound() = ResponseStatusException(HttpStatus.NOT_FOUND, "Este comercio ya no está disponible")
}
