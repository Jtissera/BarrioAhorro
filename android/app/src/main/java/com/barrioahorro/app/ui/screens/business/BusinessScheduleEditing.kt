package com.barrioahorro.app.ui.screens.business

import com.barrioahorro.app.data.remote.dto.ScheduleSlotRequestDto
import com.barrioahorro.app.data.remote.dto.ScheduleSlotResponseDto
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import com.barrioahorro.app.ui.screens.onboarding.TimeSlotUi
import java.util.UUID

private val DAY_LABELS = listOf(
    1 to "Lunes",
    2 to "Martes",
    3 to "Miércoles",
    4 to "Jueves",
    5 to "Viernes",
    6 to "Sábado",
    7 to "Domingo",
)

/** Arma la semana completa a partir de los horarios guardados. Los días sin turnos quedan cerrados. */
fun List<ScheduleSlotResponseDto>.toScheduleUi(): List<DayScheduleUi> =
    DAY_LABELS.map { (diaSemana, label) ->
        val slots = filter { it.diaSemana == diaSemana && it.activo }
            .sortedBy { it.horaInicio }
            .map { TimeSlotUi(horaInicio = it.horaInicio, horaFin = it.horaFin) }
        DayScheduleUi(diaSemana = diaSemana, label = label, enabled = slots.isNotEmpty(), slots = slots)
    }

fun List<DayScheduleUi>.toScheduleRequest(): List<ScheduleSlotRequestDto> =
    filter { it.enabled }.flatMap { day ->
        day.slots.map { slot ->
            ScheduleSlotRequestDto(
                diaSemana = day.diaSemana,
                horaInicio = slot.horaInicio,
                horaFin = slot.horaFin,
                activo = true,
            )
        }
    }

/** Devuelve un mensaje de error si algún turno es inválido, o null si la semana es válida. */
fun validateSchedule(schedule: List<DayScheduleUi>): String? {
    for (day in schedule.filter { it.enabled }) {
        // Las horas vienen como "HH:mm" con ceros a la izquierda, así que se pueden comparar como texto.
        if (day.slots.any { it.horaFin <= it.horaInicio }) {
            return "${day.label}: la hora de cierre debe ser posterior a la de apertura"
        }
        val overlaps = day.slots.sortedBy { it.horaInicio }
            .zipWithNext()
            .any { (previous, next) -> next.horaInicio < previous.horaFin }
        if (overlaps) {
            return "${day.label}: los turnos no pueden superponerse"
        }
    }
    return null
}

fun List<DayScheduleUi>.toggleDay(diaSemana: Int): List<DayScheduleUi> = map { day ->
    if (day.diaSemana != diaSemana) return@map day
    val nowEnabled = !day.enabled
    day.copy(
        enabled = nowEnabled,
        slots = if (nowEnabled && day.slots.isEmpty()) listOf(TimeSlotUi()) else day.slots,
    )
}

fun List<DayScheduleUi>.addSlot(diaSemana: Int): List<DayScheduleUi> = map { day ->
    if (day.diaSemana == diaSemana) day.copy(slots = day.slots + TimeSlotUi()) else day
}

fun List<DayScheduleUi>.removeSlot(diaSemana: Int, slotId: String): List<DayScheduleUi> = map { day ->
    if (day.diaSemana == diaSemana) day.copy(slots = day.slots.filterNot { it.id == slotId }) else day
}

fun List<DayScheduleUi>.updateSlotTime(
    diaSemana: Int,
    slotId: String,
    horaInicio: String? = null,
    horaFin: String? = null,
): List<DayScheduleUi> = map { day ->
    if (day.diaSemana != diaSemana) return@map day
    day.copy(
        slots = day.slots.map { slot ->
            if (slot.id != slotId) return@map slot
            slot.copy(horaInicio = horaInicio ?: slot.horaInicio, horaFin = horaFin ?: slot.horaFin)
        },
    )
}

fun List<DayScheduleUi>.copyMondayToAll(): List<DayScheduleUi> {
    val monday = firstOrNull { it.diaSemana == 1 } ?: return this
    return map { day ->
        if (day.diaSemana == 1) {
            day
        } else {
            day.copy(
                enabled = monday.enabled,
                slots = monday.slots.map { it.copy(id = UUID.randomUUID().toString()) },
            )
        }
    }
}
