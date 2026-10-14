package com.barrioahorro.app.domain.model

data class ValidatedAddress(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val alturaEncontrada: Boolean = true,
    val mensaje: String? = null,
)