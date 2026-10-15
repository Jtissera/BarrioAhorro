package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.DashboardActiveOffer
import com.barrioahorro.api.modules.business.dto.DashboardDayViews
import com.barrioahorro.api.modules.business.dto.DashboardResponse
import com.barrioahorro.api.modules.business.dto.DashboardReview
import com.barrioahorro.api.modules.business.dto.DashboardTopOffer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val DASHBOARD_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
private const val DAY_LETTERS = "LMMJVSD"
private const val TOP_OFFERS_LIMIT = 3
private const val RECENT_REVIEWS_LIMIT = 2
private const val ACTIVE_OFFERS_LIMIT = 3

@RestController
@RequestMapping("/api/business")
class DashboardController(
    private val businessRepository: BusinessRepository,
    private val queries: DashboardQueries,
) {

    @Transactional(readOnly = true)
    @GetMapping("/me/dashboard")
    fun getMyDashboard(authentication: Authentication): ResponseEntity<DashboardResponse> {
        val userId = authentication.principal as Long
        val business = businessRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found")
        }

        // Ventana: últimos 7 días locales, incluido hoy.
        val today = LocalDate.now(DASHBOARD_ZONE)
        val firstDay = today.minusDays(6)
        val from = firstDay.atStartOfDay(DASHBOARD_ZONE).toOffsetDateTime()
        val to = today.plusDays(1).atStartOfDay(DASHBOARD_ZONE).toOffsetDateTime()

        val counts = queries.viewsByDay(userId, from, to, DASHBOARD_ZONE.id)
        val viewsByDay = (0L..6L).map { offset ->
            val day = firstDay.plusDays(offset)
            DashboardDayViews(
                label = DAY_LETTERS[day.dayOfWeek.value - 1].toString(),
                views = counts[day] ?: 0,
            )
        }

        return ResponseEntity.ok(
            DashboardResponse(
                businessName = business.nombreNegocio?.takeIf { it.isNotBlank() } ?: "Mi negocio",
                totalViews = viewsByDay.sumOf { it.views },
                viewsByDay = viewsByDay,
                topOffers = queries.topOffers(userId, from, to, TOP_OFFERS_LIMIT)
                    .map { DashboardTopOffer(it.id, it.title, it.views) },
                recentReviews = queries.recentReviews(userId, RECENT_REVIEWS_LIMIT)
                    .map { DashboardReview(it.id, it.authorEmail, it.rating, it.comment) },
                activeOffers = queries.activeOffers(userId, ACTIVE_OFFERS_LIMIT).map {
                    DashboardActiveOffer(
                        id = it.id,
                        title = it.title,
                        unitPrice = it.unitPrice,
                        expiresLabel = expiresLabel(it.vigenciaTipo, it.vigenteHasta, today),
                    )
                },
            ),
        )
    }

    private fun expiresLabel(vigenciaTipo: String, vigenteHasta: OffsetDateTime?, today: LocalDate): String? {
        if (vigenteHasta != null) {
            val days = ChronoUnit.DAYS.between(today, vigenteHasta.atZoneSameInstant(DASHBOARD_ZONE).toLocalDate())
            return when {
                days <= 0L -> "Vence hoy"
                days == 1L -> "Vence mañana"
                else -> "Vence en $days días"
            }
        }
        return when (vigenciaTipo) {
            "HASTA_AGOTAR_STOCK" -> "Hasta agotar stock"
            "TODA_LA_SEMANA" -> "Toda la semana"
            "SOLO_HOY" -> "Solo hoy"
            else -> null
        }
    }
}