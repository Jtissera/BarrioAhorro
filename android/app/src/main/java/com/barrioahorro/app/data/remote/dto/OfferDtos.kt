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

data class Offer2x1DetailResponseDto(
    val unidadesAPagar: Short,
    val unidadesALlevar: Short,
    val precioUnitario: BigDecimal? = null,
)

data class OfferPorMayorDetailResponseDto(
    val cantidadMinima: Short,
    val precioUnitarioMayorista: BigDecimal,
    val precioUnitarioRegular: BigDecimal? = null,
)

data class OfferCantidadDetailResponseDto(
    val cantidadRequerida: Short,
    val precioUnitario: BigDecimal,
    val porcentajeDescuentoUltimaUnidad: Short,
)

data class OfferPorcentajeDetailResponseDto(
    val precioOriginal: BigDecimal,
    val porcentajeDescuento: Short,
    val precioFinal: BigDecimal? = null,
)

data class OfferResponseDto(
    val id: Long,
    val comercioId: Long,
    val nombreProducto: String,
    val fotoUrl: String? = null,
    val tipoOferta: String,
    val vigenciaTipo: String,
    val vigenteHasta: String? = null,
    val activa: Boolean = true,
    val vigente: Boolean = true,
    val createdAt: String? = null,
    val detalle2x1: Offer2x1DetailResponseDto? = null,
    val detallePorMayor: OfferPorMayorDetailResponseDto? = null,
    val detalleCantidad: OfferCantidadDetailResponseDto? = null,
    val detallePorcentaje: OfferPorcentajeDetailResponseDto? = null,
)

data class ApiErrorDto(
    val error: String?,
)

data class OfferPhotoDto(
    val url: String?,
)
