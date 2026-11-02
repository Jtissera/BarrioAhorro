package com.barrioahorro.api.modules.cuponera.dto

import java.time.LocalDate

data class CouponResponse(
    val id: Long,
    val productName: String,
    val description: String?,
    val photoUrl: String?,
    val pointsCost: Int,
    val availableUnits: Int,
    val validUntil: LocalDate,
    val businessId: Long,
    val businessName: String?,
    val category: String?,
    val distanceMeters: Int?,
)

data class CouponsPage(
    val items: List<CouponResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
)