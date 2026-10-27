package com.barrioahorro.api.modules.cuponera.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class RaffleCommitmentResponse(
    val id: Long,
    val cycleStart: LocalDate,
    val cycleEnd: LocalDate,
    val ratificationDeadline: OffsetDateTime,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val status: String,
    val prizeName: String?,
    val prizeDescription: String?,
    val prizeValue: BigDecimal?,
)

data class RatifyRaffleRequest(
    @field:NotBlank(message = "Ingresá el nombre del premio")
    @field:Size(max = 120, message = "El nombre del premio es demasiado largo")
    val prizeName: String,

    @field:NotBlank(message = "Describí el premio del sorteo")
    @field:Size(max = 255, message = "La descripción del premio es demasiado larga")
    val prizeDescription: String,

    @field:NotNull(message = "El valor del premio es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El valor del premio debe ser mayor a cero")
    @field:Digits(integer = 10, fraction = 2, message = "El valor del premio no es válido")
    val prizeValue: BigDecimal,
)

/** ratificationType: EARLY (renovación anticipada) | LATE (cuenta restringida) | null. */
data class CuponeraCycleResponse(
    val current: CuponeraCommitmentResponse?,
    val next: CuponeraCommitmentResponse?,
    val canRatify: Boolean,
    val ratificationType: String?,
)

data class RaffleCycleResponse(
    val current: RaffleCommitmentResponse?,
    val next: RaffleCommitmentResponse?,
    val canRatify: Boolean,
    val ratificationType: String?,
)

data class CommitmentOverviewResponse(
    val restrictedAccess: Boolean,
    val cuponera: CuponeraCycleResponse,
    val raffle: RaffleCycleResponse,
    val rejection: RejectionResponse? = null,
)

enum class ScanResult {
    VALIDATED,
    ALREADY_USED,
    EXPIRED,
    CANCELLED,
    OTHER_BUSINESS,
    NOT_FOUND,
    INSUFFICIENT_POINTS,
}

data class ScanRedemptionRequest(
    @field:NotBlank(message = "El código QR está vacío")
    @field:Size(max = 100, message = "El código QR no es válido")
    val code: String,
)

data class ScanRedemptionResponse(
    val result: ScanResult,
    val receiptCode: String? = null,
    val productName: String? = null,
    val photoUrl: String? = null,
    val points: Int? = null,
    /** Email enmascarado del vecino. */
    val neighbor: String? = null,
    /** Fecha de validación (VALIDATED) o de uso previo (ALREADY_USED). */
    val date: OffsetDateTime? = null,
)

data class RejectionResponse(
    /** CUPON | SORTEO */
    val kind: String,
    val title: String,
    val reason: String,
)