package com.barrioahorro.api.modules.explore

import java.time.OffsetDateTime

data class ClientProfileResponse(
    /** Parte del mail antes del @. */
    val name: String,
    val points: Int,
    val postsCount: Int,
    val reviewsCount: Int,
)

data class MyReviewResponse(
    val id: Long,
    val businessId: Long,
    val businessName: String?,
    val category: String?,
    val rating: Int,
    val comment: String,
    val createdAt: OffsetDateTime,
)

data class MyReviewsPage(
    val items: List<MyReviewResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)