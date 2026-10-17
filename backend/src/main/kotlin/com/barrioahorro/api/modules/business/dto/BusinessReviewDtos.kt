package com.barrioahorro.api.modules.business.dto

import java.time.OffsetDateTime

data class ReviewStarCount(val stars: Int, val count: Int)

data class ReviewsSummaryResponse(
    val average: Double,
    val total: Int,
    val distribution: List<ReviewStarCount>,
)

data class BusinessReviewResponse(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val createdAt: OffsetDateTime,
)

data class ReviewsPageResponse(
    val summary: ReviewsSummaryResponse,
    val items: List<BusinessReviewResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)