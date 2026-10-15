package com.barrioahorro.app.data.remote.dto

import java.math.BigDecimal

data class CreateOfferPorcentajeRequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String? = null,
    val precioOriginal: BigDecimal,
    val porcentajeDescuento: Int,
    val fotoUrl: String? = null,
)

data class CreateOfferCantidadRequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String? = null,
    val cantidadRequerida: Int,
    val precioUnitario: BigDecimal,
    val porcentajeDescuentoUltimaUnidad: Int,
    val fotoUrl: String? = null,
)

data class CreateOffer2x1RequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String? = null,
    val unidadesAPagar: Short,
    val unidadesALlevar: Short,
    val precioUnitario: BigDecimal,
    val fotoUrl: String? = null,
)

data class CreateOfferPorMayorRequestDto(
    val nombreProducto: String,
    val vigenciaTipo: String,
    val vigenteHasta: String? = null,
    val cantidadMinima: Short,
    val precioUnitarioMayorista: BigDecimal,
    val precioUnitarioRegular: BigDecimal? = null,
    val fotoUrl: String? = null,
)

data class OfferCreatedDto(
    val id: Long,
)

data class ApiErrorDto(
    val error: String?,
)

data class OfferPhotoDto(
    val url: String?,
)
