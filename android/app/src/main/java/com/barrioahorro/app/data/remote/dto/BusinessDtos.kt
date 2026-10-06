package com.barrioahorro.app.data.remote.dto

data class ScheduleSlotRequestDto(
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val activo: Boolean = true,
)

data class UpdateBusinessRequestDto(
    val businessName: String? = null,
    val categoryId: Int? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val descripcion: String? = null,
    val horarios: List<ScheduleSlotRequestDto>? = null,
)

data class ScheduleSlotResponseDto(
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val activo: Boolean,
)

data class BusinessResponseDto(
    val userId: Long,
    val businessName: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val direccion: String?,
    val latitud: Double?,
    val longitud: Double?,
    val descripcion: String?,
    val horarios: List<ScheduleSlotResponseDto>,
    val fotos: List<PhotoResponseDto>,
    val onboardingCompleted: Boolean,
)

data class PhotoResponseDto(
    val id: Long,
    val url: String,
    val orden: Int,
)

data class ReorderPhotosRequestDto(
    val photoIds: List<Long>,
)
