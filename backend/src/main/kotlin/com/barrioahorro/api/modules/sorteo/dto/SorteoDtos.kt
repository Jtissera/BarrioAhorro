package com.barrioahorro.api.modules.sorteo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

data class RatifySorteoRequest(
    @field:NotBlank(message = "Contanos qué premio vas a aportar")
    @field:Size(max = 200, message = "La descripción del premio puede tener hasta 200 caracteres")
    val premioDescripcion: String,
)

data class SorteoCommitmentResponse(
    val id: Long,
    val cicloInicio: LocalDate,
    val cicloFin: LocalDate,
    val fechaLimiteRatificacion: OffsetDateTime,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val estado: String,
    val fechaRatificacion: OffsetDateTime?,
    val premioDescripcion: String?,
    val puedeRatificar: Boolean,
)