package com.barrioahorro.app.data.explore

import kotlinx.serialization.Serializable

@Serializable
data class RedeemCouponRequestDto(val couponId: Long)

@Serializable
data class RedemptionDto(
    val id: Long,
    val receiptCode: String,
    val code: String?,
    /** PENDIENTE_VALIDACION | VALIDADO | CANCELADO | VENCIDO */
    val status: String,
    val productName: String,
    val photoUrl: String?,
    val businessId: Long,
    val businessName: String?,
    val pointsCost: Int,
    val redeemedAt: String,
    val validatedAt: String?,
    val validUntil: String,
)