package com.barrioahorro.app.data.remote.dto

import java.math.BigDecimal

data class CreateOfferPorcentajeRequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String,
    val precioOriginal: BigDecimal,
    val porcentajeDescuento: Int,
)

data class CreateOfferCantidadRequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String,
    val cantidadRequerida: Int,
    val precioUnitario: BigDecimal,
    val porcentajeDescuentoUltimaUnidad: Int,
)

data class OfferCreatedDto(
    val id: Long,
)

data class ApiErrorDto(
    val error: String?,
)

