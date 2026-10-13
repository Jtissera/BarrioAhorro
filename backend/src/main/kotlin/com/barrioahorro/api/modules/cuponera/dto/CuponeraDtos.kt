package com.barrioahorro.api.modules.cuponera.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

private const val NOT_BLANK_REGEX = "(?s).*\\S.*"

data class CuponeraProductRequest(
    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "El nombre del producto no puede estar vacío")
    @field:Size(max = 120, message = "El nombre del producto es demasiado largo")
    val nombreProducto: String,

    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "La descripción no puede estar vacía")
    @field:Size(max = 255, message = "La descripción es demasiado larga")
    val descripcion: String? = null,

    val fotoUrl: String? = null,

    /** Precio de venta en el comercio, en pesos ("Valor en tu comercio"). */
    @field:NotNull(message = "El valor del producto es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El valor del producto debe ser mayor a cero")
    @field:Digits(integer = 10, fraction = 2, message = "El valor del producto no es válido")
    val valorComercio: BigDecimal,

    /** "Unidades disponibles": stock comprometido para el período. */
    @field:Min(value = 1, message = "Las unidades disponibles deben ser al menos 1")
    @field:Max(value = 9999, message = "Las unidades disponibles no pueden superar 9999")
    val unidadesDisponibles: Int,
)

data class RatifyCuponeraRequest(
    @field:Size(min = 2, max = 4, message = "Tenés que aportar entre 2 y 4 productos")
    @field:Valid
    val productos: List<CuponeraProductRequest>,
)

data class CuponeraProductResponse(
    val id: Long,
    val nombreProducto: String,
    val descripcion: String?,
    val fotoUrl: String?,
    val valorComercio: BigDecimal?,
    val costoPuntos: Int,
    val unidadesDisponibles: Int,
    val unidadesCanjeadas: Int,
)

data class CuponeraCommitmentResponse(
    val id: Long,
    val periodoInicio: LocalDate,
    val periodoFin: LocalDate,
    val fechaLimiteRatificacion: OffsetDateTime,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val estado: String,
    val fechaRatificacion: OffsetDateTime?,
    val puedeRatificar: Boolean,
    val minProductos: Int,
    val maxProductos: Int,
    val pesosPorPunto: Int,
    val productos: List<CuponeraProductResponse>,
    val sugerencias: List<String>,
)