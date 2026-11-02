package com.barrioahorro.app.data.explore

data class FeedbackStatusDto(
    val answered: Boolean,
    val received: Boolean?,
    val pointsAwarded: Int,
)

data class ReportRedemptionRequestDto(
    /** PEDIDO_INCOMPLETO | CALIDAD | HORARIO | OTRO */
    val reason: String,
    val description: String,
)