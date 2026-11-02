package com.barrioahorro.app.data.explore

import kotlinx.serialization.Serializable

@Serializable
data class CouponDto(
    val id: Long,
    val productName: String,
    val description: String?,
    val photoUrl: String?,
    val pointsCost: Int,
    val availableUnits: Int,
    val validUntil: String,
    val businessId: Long,
    val businessName: String?,
    val category: String?,
    val distanceMeters: Int?,
)

@Serializable
data class CouponsPageDto(
    val items: List<CouponDto>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
)

data class CouponQuery(
    val latitude: Double?,
    val longitude: Double?,
    val radiusKm: Double?,
    val categoryIds: List<Int>,
    val text: String?,
    val order: String,
)