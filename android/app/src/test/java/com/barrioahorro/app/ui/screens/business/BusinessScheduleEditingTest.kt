package com.barrioahorro.app.ui.screens.business

import com.barrioahorro.app.data.remote.dto.ScheduleSlotRequestDto
import com.barrioahorro.app.data.remote.dto.ScheduleSlotResponseDto
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import com.barrioahorro.app.ui.screens.onboarding.TimeSlotUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessScheduleEditingTest {

    @Test
    fun `toScheduleUi arma los 7 dias y deja cerrados los que no tienen turnos`() {
        val saved = listOf(ScheduleSlotResponseDto(diaSemana = 2, horaInicio = "09:00", horaFin = "18:00", activo = true))

        val schedule = saved.toScheduleUi()

        assertEquals((1..7).toList(), schedule.map { it.diaSemana })
        assertTrue(schedule.single { it.diaSemana == 2 }.enabled)
        assertFalse(schedule.single { it.diaSemana == 1 }.enabled)
        assertTrue(schedule.single { it.diaSemana == 1 }.slots.isEmpty())
    }

    @Test
    fun `toScheduleUi agrupa los turnos partidos ordenados por hora de inicio`() {
        val saved = listOf(
            ScheduleSlotResponseDto(diaSemana = 1, horaInicio = "16:00", horaFin = "20:00", activo = true),
            ScheduleSlotResponseDto(diaSemana = 1, horaInicio = "08:00", horaFin = "12:00", activo = true),
        )

        val monday = saved.toScheduleUi().first()

        assertEquals(listOf("08:00", "16:00"), monday.slots.map { it.horaInicio })
    }

    @Test
    fun `toScheduleRequest solo envia los dias abiertos`() {
        val schedule = listOf(
            DayScheduleUi(1, "Lunes", enabled = true, slots = listOf(TimeSlotUi(horaInicio = "09:00", horaFin = "13:00"))),
            DayScheduleUi(2, "Martes", enabled = false, slots = listOf(TimeSlotUi(horaInicio = "09:00", horaFin = "13:00"))),
        )

        assertEquals(
            listOf(ScheduleSlotRequestDto(diaSemana = 1, horaInicio = "09:00", horaFin = "13:00", activo = true)),
            schedule.toScheduleRequest(),
        )
    }

    @Test
    fun `cargar y volver a enviar los horarios no los modifica`() {
        val saved = listOf(
            ScheduleSlotResponseDto(diaSemana = 3, horaInicio = "08:00", horaFin = "12:00", activo = true),
            ScheduleSlotResponseDto(diaSemana = 3, horaInicio = "16:00", horaFin = "20:00", activo = true),
        )

        val request = saved.toScheduleUi().toScheduleRequest()

        assertEquals(saved.map { Triple(it.diaSemana, it.horaInicio, it.horaFin) }, request.map { Triple(it.diaSemana, it.horaInicio, it.horaFin) })
    }

    @Test
    fun `validateSchedule acepta turnos validos y contiguos`() {
        val schedule = listOf(
            DayScheduleUi(
                1, "Lunes", enabled = true,
                slots = listOf(TimeSlotUi(horaInicio = "08:00", horaFin = "12:00"), TimeSlotUi(horaInicio = "12:00", horaFin = "18:00")),
            ),
        )

        assertNull(validateSchedule(schedule))
    }

    @Test
    fun `validateSchedule rechaza un turno que cierra antes de abrir`() {
        val schedule = listOf(
            DayScheduleUi(1, "Lunes", enabled = true, slots = listOf(TimeSlotUi(horaInicio = "18:00", horaFin = "09:00"))),
        )

        assertNotNull(validateSchedule(schedule))
    }

    @Test
    fun `validateSchedule rechaza turnos superpuestos`() {
        val schedule = listOf(
            DayScheduleUi(
                2, "Martes", enabled = true,
                slots = listOf(TimeSlotUi(horaInicio = "12:00", horaFin = "18:00"), TimeSlotUi(horaInicio = "08:00", horaFin = "13:00")),
            ),
        )

        assertNotNull(validateSchedule(schedule))
    }

    @Test
    fun `validateSchedule ignora los dias cerrados`() {
        val schedule = listOf(
            DayScheduleUi(1, "Lunes", enabled = false, slots = listOf(TimeSlotUi(horaInicio = "18:00", horaFin = "09:00"))),
        )

        assertNull(validateSchedule(schedule))
    }

    @Test
    fun `toggleDay al abrir un dia sin turnos le agrega uno por defecto`() {
        val schedule = listOf(DayScheduleUi(6, "Sábado", enabled = false, slots = emptyList()))

        val day = schedule.toggleDay(6).single()

        assertTrue(day.enabled)
        assertEquals(1, day.slots.size)
    }

    @Test
    fun `copyMondayToAll copia los turnos del lunes con ids nuevos`() {
        val mondaySlot = TimeSlotUi(horaInicio = "10:00", horaFin = "14:00")
        val schedule = listOf(
            DayScheduleUi(1, "Lunes", enabled = true, slots = listOf(mondaySlot)),
            DayScheduleUi(2, "Martes", enabled = false, slots = emptyList()),
        )

        val tuesday = schedule.copyMondayToAll()[1]

        assertTrue(tuesday.enabled)
        assertEquals("10:00", tuesday.slots.single().horaInicio)
        assertFalse(tuesday.slots.single().id == mondaySlot.id)
    }

    @Test
    fun `updateSlotTime cambia solo el turno indicado`() {
        val target = TimeSlotUi(horaInicio = "09:00", horaFin = "12:00")
        val other = TimeSlotUi(horaInicio = "15:00", horaFin = "19:00")
        val schedule = listOf(DayScheduleUi(1, "Lunes", enabled = true, slots = listOf(target, other)))

        val slots = schedule.updateSlotTime(1, target.id, horaFin = "13:00").single().slots

        assertEquals("13:00", slots[0].horaFin)
        assertEquals(other, slots[1])
    }
}
