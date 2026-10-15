package com.barrioahorro.app.data.remote.dto

import com.barrioahorro.app.domain.model.ActiveOffer
import com.barrioahorro.app.domain.model.BusinessDashboard
import com.barrioahorro.app.domain.model.BusinessReview
import com.barrioahorro.app.domain.model.DayViews
import com.barrioahorro.app.domain.model.TopOffer
import java.text.NumberFormat
import java.util.Locale

// Agregar a estos DTOs la anotación que usen los demás (@Serializable / @JsonClass / @SerializedName)
// si el converter de Retrofit del proyecto la necesita.

data class DashboardResponseDto(
    val businessName: String,
    val totalViews: Int,
    val viewsByDay: List<DayViewsDto>,
    val topOffers: List<TopOfferDto>,
    val recentReviews: List<DashboardReviewDto>,
    val activeOffers: List<ActiveOfferDto>
)

data class DayViewsDto(val label: String, val views: Int)

data class TopOfferDto(val id: Long, val title: String, val views: Int)

data class DashboardReviewDto(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val comment: String
)

data class ActiveOfferDto(
    val id: Long,
    val title: String,
    val unitPrice: Double?,
    val expiresLabel: String?
)

fun DashboardResponseDto.toDomain() = BusinessDashboard(
    businessName = businessName,
    totalViews = totalViews,
    viewsByDay = viewsByDay.map { DayViews(it.label, it.views) },
    topOffers = topOffers.map { TopOffer(it.id.toString(), it.title, it.views) },
    recentReviews = recentReviews.map { BusinessReview(it.id.toString(), it.authorName, it.rating, it.comment) },
    activeOffers = activeOffers.map {
        ActiveOffer(it.id.toString(), it.title, it.unitPrice?.let(::formatPrice), it.expiresLabel)
    }
)

private fun formatPrice(value: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"))
    val hasCents = value % 1.0 != 0.0
    format.minimumFractionDigits = if (hasCents) 2 else 0
    format.maximumFractionDigits = 2
    return "$" + format.format(value)
}