package com.barrioahorro.app.data.explore

import java.math.BigDecimal

data class ExploreOfferDto(
    val id: Long,
    val productName: String,
    val photoUrl: String?,
    /** PORCENTAJE | DOS_POR_UNO | CANTIDAD_ULTIMA_UNIDAD | POR_MAYOR */
    val type: String,
    val validityType: String,
    val validUntil: String?,
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
    val distanceMeters: Int?,
    val confirmations: Int,
    val favorite: Boolean,
    /** false si la oferta ya terminó. */
    val live: Boolean = true,
)

data class ExploreOffersPageDto(
    val items: List<ExploreOfferDto>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
)

data class ClientSummaryDto(val points: Int)

data class ExploreCategory(val id: Int, val name: String)

data class ExploreBusinessDto(
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