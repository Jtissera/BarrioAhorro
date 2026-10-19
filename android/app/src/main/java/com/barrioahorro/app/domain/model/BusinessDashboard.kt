package com.barrioahorro.app.domain.model

data class BusinessDashboard(
    val businessName: String,
    val totalViews: Int,
    val viewsByDay: List<DayViews>,
    val topOffers: List<TopOffer>,
    val recentReviews: List<BusinessReview>,
    val activeOffers: List<ActiveOffer>
)

data class DayViews(val label: String, val views: Int)

data class TopOffer(val id: String, val title: String, val views: Int)

data class BusinessReview(
    val id: String,
    val authorName: String,
    val rating: Int,
    val comment: String
)

data class ActiveOffer(
    val id: String,
    val title: String,
    val priceLabel: String?,
    val expiresLabel: String?,
    val badge: String? = null,
)