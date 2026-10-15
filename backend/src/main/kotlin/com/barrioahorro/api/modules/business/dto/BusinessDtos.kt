package com.barrioahorro.api.modules.business.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

private const val TIME_REGEX = "^([01]\\d|2[0-3]):[0-5]\\d$"

// Los campos de texto son opcionales (PATCH), pero si vienen no pueden estar en blanco.
private const val NOT_BLANK_REGEX = "(?s).*\\S.*"

data class ScheduleSlotRequest(
    @field:Min(value = 1, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)")
    @field:Max(value = 7, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)")
    val diaSemana: Int,
    @field:Pattern(regexp = TIME_REGEX, message = "La hora de inicio debe tener formato HH:mm")
    val horaInicio: String,
    @field:Pattern(regexp = TIME_REGEX, message = "La hora de fin debe tener formato HH:mm")
    val horaFin: String,
    val activo: Boolean = true,
)

data class UpdateBusinessRequest(
    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "El nombre del negocio no puede estar vacío")
    val businessName: String? = null,
    val categoryId: Int? = null,
    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "La dirección no puede estar vacía")
    val direccion: String? = null,
    @field:DecimalMin(value = "-90.0", message = "La latitud no es válida")
    @field:DecimalMax(value = "90.0", message = "La latitud no es válida")
    val latitud: Double? = null,
    @field:DecimalMin(value = "-180.0", message = "La longitud no es válida")
    @field:DecimalMax(value = "180.0", message = "La longitud no es válida")
    val longitud: Double? = null,
    @field:Pattern(regexp = NOT_BLANK_REGEX, message = "La descripción no puede estar vacía")
    val descripcion: String? = null,
    @field:Valid
    val horarios: List<ScheduleSlotRequest>? = null,
    /** Solo `true` tiene efecto: cierra el onboarding si están todos los datos. */
    val completarOnboarding: Boolean? = null,
)

data class ScheduleSlotResponse(
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val activo: Boolean,
)

data class BusinessResponse(
    val userId: Long,
    val businessName: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val direccion: String?,
    val latitud: Double?,
    val longitud: Double?,
    val descripcion: String?,
    val horarios: List<ScheduleSlotResponse>,
    val fotos: List<PhotoResponse>,
    val onboardingCompleted: Boolean,
)



data class DashboardResponse(
    val businessName: String,
    val totalViews: Int,
    val viewsByDay: List<DashboardDayViews>,
    val topOffers: List<DashboardTopOffer>,
    val recentReviews: List<DashboardReview>,
    val activeOffers: List<DashboardActiveOffer>,
)

data class DashboardDayViews(val label: String, val views: Int)

data class DashboardTopOffer(val id: Long, val title: String, val views: Int)

data class DashboardReview(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val comment: String,
)

data class DashboardActiveOffer(
    val id: Long,
    val title: String,
    val unitPrice: BigDecimal?,
    val expiresLabel: String?,
)