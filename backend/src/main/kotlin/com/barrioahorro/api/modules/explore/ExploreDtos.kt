package com.barrioahorro.api.modules.explore

import java.math.BigDecimal
import java.time.OffsetDateTime

data class ExploreOfferResponse(
    val id: Long,
    val productName: String,
    val photoUrl: String?,
    /** PORCENTAJE | DOS_POR_UNO | CANTIDAD_ULTIMA_UNIDAD | POR_MAYOR */
    val type: String,
    val validityType: String,
    val validUntil: OffsetDateTime?,
    /** Último cierre del comercio hoy ("20:00"), o null si hoy no abre. */
    val closesToday: String?,
    val businessId: Long,
    val businessName: String?,
    val category: String?,
    val businessOpen: Boolean,
    val price: BigDecimal?,
    val originalPrice: BigDecimal?,
    val percent: Int?,
    val take: Int?,
    val pay: Int?,
    val requiredQuantity: Int?,
    val lastUnitDiscount: Int?,
    val minQuantity: Int?,
    /** null si el vecino no compartió su ubicación. */
    val distanceMeters: Int?,
    val confirmations: Int,
    val favorite: Boolean,
)

data class ExploreOffersPage(
    val items: List<ExploreOfferResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
)

data class ExploreBusinessResponse(
    val id: Long,
    val name: String?,
    val category: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val open: Boolean,
    val activeOffers: Int,
    val photoUrl: String?,
)

data class ClientSummaryResponse(val points: Int)