package com.barrioahorro.api.modules.cuponera.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RedemptionFeedbackResponse(
    val answered: Boolean,
    /** true = recibió, false = reportó, null = todavía no respondió. */
    val received: Boolean?,
    val pointsAwarded: Int,
)

data class ReportRedemptionRequest(
    @field:Pattern(
        regexp = "PEDIDO_INCOMPLETO|CALIDAD|HORARIO|OTRO",
        message = "El motivo no es válido",
    )
    val reason: String,

    @field:NotBlank(message = "Contanos qué pasó")
    @field:Size(max = 300, message = "El detalle es demasiado largo")
    val description: String,
)