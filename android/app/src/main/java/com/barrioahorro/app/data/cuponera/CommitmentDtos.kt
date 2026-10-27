package com.barrioahorro.app.data.cuponera

data class RaffleCommitmentDto(
    val id: Long,
    val cycleStart: String,
    val cycleEnd: String,
    val ratificationDeadline: String,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val status: String,
    val prizeName: String?,
    val prizeDescription: String?,
    val prizeValue: Double?,
)

data class CuponeraCycleDto(
    val current: CuponeraCommitmentDto?,
    val next: CuponeraCommitmentDto?,
    val canRatify: Boolean,
    /** EARLY | LATE | null */
    val ratificationType: String?,
)

data class RaffleCycleDto(
    val current: RaffleCommitmentDto?,
    val next: RaffleCommitmentDto?,
    val canRatify: Boolean,
    val ratificationType: String?,
)

data class CommitmentOverviewDto(
    val restrictedAccess: Boolean,
    val cuponera: CuponeraCycleDto,
    val raffle: RaffleCycleDto,
    val rejection: RejectionDto? = null,
)

data class RatifyRaffleRequestDto(
    val prizeName: String,
    val prizeDescription: String,
    val prizeValue: Double,
)
data class ScanRedemptionRequestDto(val code: String)

/** `result` llega como String: si el backend agrega un valor nuevo, la app no rompe. */
data class ScanRedemptionDto(
    val result: String?,
    val receiptCode: String?,
    val productName: String?,
    val photoUrl: String?,
    val points: Int?,
    val neighbor: String?,
    val date: String?,
)

data class RejectionDto(
    /** CUPON | SORTEO */
    val kind: String,
    val title: String,
    val reason: String,
)

