package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.BusinessReviewResponse
import com.barrioahorro.api.modules.business.dto.ReviewStarCount
import com.barrioahorro.api.modules.business.dto.ReviewsPageResponse
import com.barrioahorro.api.modules.business.dto.ReviewsSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import kotlin.math.round

private const val MAX_PAGE_SIZE = 50
private const val MAX_STARS = 5

@Service
class BusinessReviewService(
    private val businessRepository: BusinessRepository,
    private val reviewQueries: BusinessReviewQueries,
) {

    @Transactional(readOnly = true)
    fun getMyReviews(userId: Long, page: Int, size: Int): ReviewsPageResponse {
        if (page < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa")
        }
        if (size !in 1..MAX_PAGE_SIZE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y $MAX_PAGE_SIZE")
        }
        if (!businessRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los comercios pueden ver sus reseñas")
        }

        val rows = reviewQueries.findPage(userId, limit = size + 1, offset = page.toLong() * size)

        return ReviewsPageResponse(
            summary = buildSummary(reviewQueries.countByStars(userId)),
            items = rows.take(size).map {
                BusinessReviewResponse(
                    id = it.id,
                    authorName = it.authorEmail.substringBefore('@'),
                    rating = it.rating,
                    comment = it.comment,
                    createdAt = it.createdAt,
                )
            },
            page = page,
            size = size,
            hasNext = rows.size > size,
        )
    }

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
}