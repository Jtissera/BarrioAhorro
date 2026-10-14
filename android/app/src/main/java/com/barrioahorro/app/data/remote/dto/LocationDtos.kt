package com.barrioahorro.app.data.remote.dto

data class ValidateAddressRequestDto(
    val direccion: String,
    val provincia: String?,
    val localidad: String? = null
)

data class ValidateAddressResponseDto(
    val direccionNormalizada: String,
    val latitud: Double,
    val longitud: Double,
    val alturaEncontrada: Boolean = true,
    val mensaje: String? = null,
)

data class AddressErrorDto(
    val error: String?,
)