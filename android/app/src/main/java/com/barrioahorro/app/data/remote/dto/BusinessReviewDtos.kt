package com.barrioahorro.app.data.remote.dto

data class ReviewStarCountDto(val stars: Int, val count: Int)

data class ReviewsSummaryDto(
    val average: Double,
    val total: Int,
    val distribution: List<ReviewStarCountDto>,
)

data class BusinessReviewDto(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
)

data class ReviewsPageDto(
    val summary: ReviewsSummaryDto,
    val items: List<BusinessReviewDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class ActiveOfferItemDto(
    val offer: OfferResponseDto,
    val confirmationsCount: Int,
)

data class ActiveOffersDto(
    val activeCount: Int,
    val items: List<ActiveOfferItemDto>,
)