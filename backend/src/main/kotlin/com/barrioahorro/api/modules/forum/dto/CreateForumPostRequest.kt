package com.barrioahorro.api.modules.forum.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateForumPostRequest(
    @field:NotNull(message = "El comercio es obligatorio")
    val comercioId: Long?,

    @field:NotBlank(message = "El texto es obligatorio")
    @field:Size(max = 300, message = "El texto no puede superar los 300 caracteres")
    val texto: String?,

    @field:Positive(message = "El precio debe ser mayor a cero")
    val precioObservado: BigDecimal? = null,

    val direccion: String? = null,

    val latitud: Double? = null,

    val longitud: Double? = null,
)
