package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.domain.model.ActiveOffer
import com.barrioahorro.app.domain.model.BusinessDashboard
import com.barrioahorro.app.domain.model.BusinessReview
import com.barrioahorro.app.domain.model.DayViews
import com.barrioahorro.app.domain.model.TopOffer
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.Result
interface DashboardRepository {
    suspend fun getDashboard(): Result<BusinessDashboard>
}

/** EJEMPLO HAY QUE CONECTAR*/
val sampleDashboard = BusinessDashboard(
    businessName = "Verdulería Don Pepe",
    totalViews = 1248,
    viewsByDay = listOf(
        DayViews("L", 148), DayViews("M", 176), DayViews("M", 162),
        DayViews("J", 190), DayViews("V", 231), DayViews("S", 205), DayViews("D", 136)
    ),
    topOffers = listOf(
        TopOffer("1", "Tomate perita 1 kg", 412),
        TopOffer("2", "Combo verduras de estación", 305),
        TopOffer("3", "Banana ecuador 1 kg", 198)
    ),
    recentReviews = listOf(
        BusinessReview("1", "Lucía R.", 5, "Todo fresco y muy buen precio, volveré."),
        BusinessReview("2", "Martín G.", 4, "Buena atención, la oferta llegó justo.")
    ),
    activeOffers = listOf(
        ActiveOffer("1", "Tomate perita 1 kg", "$1.200", "Vence en 2 días"),
        ActiveOffer("2", "Combo verduras de estación", "$3.500", "Vence en 5 días"),
        ActiveOffer("3", "Banana ecuador 1 kg", "$1.800", null)
    )
)

class FakeDashboardRepository @Inject constructor() : DashboardRepository {
    override suspend fun getDashboard(): Result<BusinessDashboard> {
        delay(600)
        return Result.success(sampleDashboard)
    }
}