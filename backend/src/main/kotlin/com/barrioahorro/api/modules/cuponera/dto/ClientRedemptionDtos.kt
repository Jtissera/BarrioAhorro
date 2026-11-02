package com.barrioahorro.api.modules.cuponera.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class RedeemCouponRequest(val couponId: Long)

data class RedemptionResponse(
    val id: Long,
    val receiptCode: String,
    /** UUID que va dentro del QR. Solo viene mientras el canje está pendiente. */
    val code: String?,
    /** PENDIENTE_VALIDACION | VALIDADO | CANCELADO | VENCIDO */
    val status: String,
    val productName: String,
    val photoUrl: String?,
    val businessId: Long,
    val businessName: String?,
    val pointsCost: Int,
    val redeemedAt: OffsetDateTime,
    val validatedAt: OffsetDateTime?,
    val validUntil: LocalDate,
)