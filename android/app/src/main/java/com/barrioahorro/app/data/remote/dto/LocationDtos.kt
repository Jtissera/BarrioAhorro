package com.barrioahorro.app.data.remote.dto

data class ValidateAddressRequestDto(
    val direccion: String,
    val provincia: String?,
)

data class ValidateAddressResponseDto(
    val direccionNormalizada: String,
    val latitud: Double,
    val longitud: Double,
)

data class AddressErrorDto(
    val error: String?,
)