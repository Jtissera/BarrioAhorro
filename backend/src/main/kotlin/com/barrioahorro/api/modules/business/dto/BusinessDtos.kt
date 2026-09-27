package com.barrioahorro.api.modules.business.dto

data class ScheduleSlotRequest(
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val activo: Boolean = true,
)

data class UpdateBusinessRequest(
    val businessName: String? = null,
    val categoryId: Int? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val descripcion: String? = null,
    val horarios: List<ScheduleSlotRequest>? = null,
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
    val onboardingCompleted: Boolean,
)