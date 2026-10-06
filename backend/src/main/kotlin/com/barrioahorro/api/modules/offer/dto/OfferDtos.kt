package com.barrioahorro.api.modules.offer.dto

import com.barrioahorro.api.modules.offer.enum.TipoBeneficio
import com.barrioahorro.api.modules.offer.enum.TipoVigencia
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.OffsetDateTime
import jakarta.validation.constraints.Max

data class CreateOffer2x1Request(
    @field:NotBlank(message = "El nombre del producto no puede estar vacío")
    val nombreProducto: String,

    val fotoUrl: String? = null,

    @field:NotNull(message = "El tipo de vigencia es obligatorio")
    val vigenciaTipo: TipoVigencia,

    val vigenteHasta: OffsetDateTime? = null,

    @field:NotNull(message = "Las unidades a pagar son obligatorias")
    @field:Min(value = 1, message = "Las unidades a pagar deben ser al menos 1")
    val unidadesAPagar: Short,

    @field:NotNull(message = "Las unidades a llevar son obligatorias")
    @field:Min(value = 2, message = "Las unidades a llevar deben ser al menos 2")
    val unidadesALlevar: Short,

    @field:DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
    val precioUnitario: BigDecimal? = null,
)

data class CreateOfferPorMayorRequest(
    @field:NotBlank(message = "El nombre del producto no puede estar vacío")
    val nombreProducto: String,

    val fotoUrl: String? = null,

    @field:NotNull(message = "El tipo de vigencia es obligatorio")
    val vigenciaTipo: TipoVigencia,

    val vigenteHasta: OffsetDateTime? = null,

    @field:NotNull(message = "La cantidad mínima requerida es obligatoria")
    @field:Min(value = 2, message = "La cantidad mínima debe ser al menos 2 unidades")
    val cantidadMinima: Short,

    @field:NotNull(message = "El precio unitario mayorista es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El precio mayorista debe ser mayor a cero")
    val precioUnitarioMayorista: BigDecimal,

    @field:DecimalMin(value = "0.01", message = "El precio regular debe ser mayor a cero")
    val precioUnitarioRegular: BigDecimal? = null,
)

data class CreateOfferCantidadRequest(
    @field:NotBlank(message = "El nombre del producto no puede estar vacío")
    val nombreProducto: String,

    val fotoUrl: String? = null,

    @field:NotNull(message = "El tipo de vigencia es obligatorio")
    val vigenciaTipo: TipoVigencia,

    val vigenteHasta: OffsetDateTime? = null,

    @field:NotNull(message = "La cantidad requerida es obligatoria")
    @field:Min(value = 2, message = "La cantidad requerida debe ser al menos 2 unidades")
    val cantidadRequerida: Short,

    @field:NotNull(message = "El precio unitario es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
    val precioUnitario: BigDecimal,

    @field:NotNull(message = "El porcentaje de descuento es obligatorio")
    @field:Min(value = 1, message = "El descuento debe ser de al menos 1%")
    @field:Max(value = 99, message = "El descuento no puede ser del 100%: para eso usá una oferta 2x1/3x2")
    val porcentajeDescuentoUltimaUnidad: Short,
)

data class Offer2x1DetailResponse(
    val unidadesAPagar: Short,
    val unidadesALlevar: Short,
    val precioUnitario: BigDecimal?,
)

data class OfferPorMayorDetailResponse(
    val cantidadMinima: Short,
    val precioUnitarioMayorista: BigDecimal,
    val precioUnitarioRegular: BigDecimal?,
)

data class OfferCantidadDetailResponse(
    val cantidadRequerida: Short,
    val precioUnitario: BigDecimal,
    val porcentajeDescuentoUltimaUnidad: Short,
)

data class OfferResponse(
    val id: Long,
    val comercioId: Long,
    val nombreProducto: String,
    val fotoUrl: String?,
    val tipoOferta: TipoBeneficio,
    val vigenciaTipo: TipoVigencia,
    val vigenteHasta: OffsetDateTime?,
    val activa: Boolean,
    val vigente: Boolean,
    val createdAt: OffsetDateTime,
    val detalle2x1: Offer2x1DetailResponse? = null,
    val detallePorMayor: OfferPorMayorDetailResponse? = null,
    val detalleCantidad: OfferCantidadDetailResponse? = null,
)
