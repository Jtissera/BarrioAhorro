package com.barrioahorro.api.modules.location.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ValidateAddressRequest(
    @field:NotBlank(message = "La dirección es obligatoria")
    @field:Size(max = 200, message = "La dirección es demasiado larga")
    val direccion: String,

    @field:Size(max = 100, message = "La localidad es demasiado larga")
    val localidad: String? = null,

    val provincia: String? = null,
)


data class ValidateAddressResponse(
    val direccionNormalizada: String,
    val latitud: Double,
    val longitud: Double,
    val alturaEncontrada: Boolean = true,
    val mensaje: String? = null,
)


data class AddressErrorResponse(
    val error: AddressErrorCode,
)

enum class AddressErrorCode {
    ADDRESS_NOT_FOUND,
    MISSING_NUMBER,
    GEOREF_UNAVAILABLE,
}