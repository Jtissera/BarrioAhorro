package com.barrioahorro.api.modules.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

enum class PublicationKind { CUPON, SORTEO }

data class AdminPublicationResponse(
    val id: Long,
    val kind: String,
    val businessName: String?,
    val businessAddress: String?,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val submittedAt: OffsetDateTime?,
    val points: Int?,
    val stock: Int?,
    val amount: BigDecimal?,
)

data class RejectRequest(
    @field:NotBlank(message = "Ingresá el motivo del rechazo")
    @field:Size(min = 15, max = 300, message = "El motivo debe tener entre 15 y 300 caracteres")
    val motivo: String,
)