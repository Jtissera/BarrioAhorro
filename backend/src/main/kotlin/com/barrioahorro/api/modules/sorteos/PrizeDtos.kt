package com.barrioahorro.api.modules.sorteos

import java.time.OffsetDateTime

data class WinnerNoticeResponse(
    val ticketId: Long,
    val raffleTitle: String,
    val prizeName: String,
    val prizePhotoUrl: String?,
    val businessName: String?,
)

data class PrizeDetailResponse(
    val ticketId: Long,
    val ticketNumber: Long,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessName: String?,
    /** CANJEABLE | CANJEADO | VENCIDO */
    val status: String,
    val expiresAt: OffsetDateTime,
    val redeemedAt: OffsetDateTime?,
    /** Solo si está CANJEABLE. */
    val qrPayload: String?,
)

data class ScanCodeRequest(val code: String)

data class PrizeScanResult(
    val valid: Boolean,
    /** INVALID_CODE | NOT_WINNER | OTHER_BUSINESS | ALREADY_REDEEMED | EXPIRED */
    val reason: String?,
    val message: String,
    val prizeName: String? = null,
    val prizeDescription: String? = null,
    val prizePhotoUrl: String? = null,
    val ticketNumber: Long? = null,
    val winnerName: String? = null,
    val expiresAt: OffsetDateTime? = null,
)