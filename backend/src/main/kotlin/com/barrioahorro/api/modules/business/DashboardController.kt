package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.DashboardActiveOffer
import com.barrioahorro.api.modules.business.dto.DashboardDayViews
import com.barrioahorro.api.modules.business.dto.DashboardOfferShare
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
private const val PIE_TOP_LIMIT = 5
private const val PIE_OTHERS_LABEL = "Otras"
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
        // El comercio sale siempre del token, nunca de un parámetro del cliente.
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

        val offerViews = queries.offerViews(userId)

        return ResponseEntity.ok(
            DashboardResponse(
                businessName = business.nombreNegocio?.takeIf { it.isNotBlank() } ?: "Mi negocio",
                totalViews = viewsByDay.sumOf { it.views },
                viewsByDay = viewsByDay,
                topOffers = offerViews.take(TOP_OFFERS_LIMIT)
                    .map { DashboardTopOffer(it.id, it.title, it.views) },
                offerViewsPie = buildOfferPie(offerViews),
                recentReviews = queries.recentReviews(userId, RECENT_REVIEWS_LIMIT)
                    .map { DashboardReview(it.id, it.authorEmail, it.rating, it.comment) },
                activeOffers = queries.activeOffers(userId, ACTIVE_OFFERS_LIMIT).map {
                    DashboardActiveOffer(
                        id = it.id,
                        title = it.title,
                        unitPrice = it.unitPrice,
                        expiresLabel = expiresLabel(it.vigenciaTipo, it.vigenteHasta, today),
                        badge = badgeFor(it),
                    )
                },
            ),
        )
    }

    /**
     * Top 5 por visualizaciones y, si hay más ofertas, una porción "Otras" con la suma del resto.
     * [rows] viene ordenado de mayor a menor y solo con ofertas que tienen visualizaciones.
     * Con menos de 5 ofertas no hay "Otras"; sin visualizaciones devuelve lista vacía.
     */
    private fun buildOfferPie(rows: List<TopOfferRow>): List<DashboardOfferShare> {
        val top = rows.take(PIE_TOP_LIMIT)
            .map { DashboardOfferShare(id = it.id, title = it.title, views = it.views, others = false) }
        val rest = rows.drop(PIE_TOP_LIMIT)
        if (rest.isEmpty()) return top

        return top + DashboardOfferShare(
            id = null,
            title = PIE_OTHERS_LABEL,
            views = rest.sumOf { it.views },
            others = true,
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

    private fun badgeFor(r: ActiveOfferRow): String? = when (r.tipo) {
        "PORCENTAJE" -> r.porcentaje?.let { "-$it%" }
        "DOS_POR_UNO" -> if (r.llevar != null && r.pagar != null) "${r.llevar}×${r.pagar}" else null
        "CANTIDAD_ULTIMA_UNIDAD" ->
            if (r.cantidadRequerida != null && r.descuentoUltima != null) "${r.cantidadRequerida}ª al ${r.descuentoUltima}%" else null
        "POR_MAYOR" -> "X mayor"
        else -> null
    }
}