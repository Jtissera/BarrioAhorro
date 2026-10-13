package com.barrioahorro.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.onboarding.OnboardingPrimaryButton
import com.barrioahorro.app.ui.components.onboarding.OnboardingTopBar
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = { TimePicker(state = state) },
    )
}

private fun parseTime(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 9) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

@Composable
private fun TimeButton(label: String, time: String, onTimeSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showDialog = true }) {
        Text(time)
    }

    if (showDialog) {
        val (h, m) = parseTime(time)
        TimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismiss = { showDialog = false },
            onConfirm = { hour, minute ->
                onTimeSelected(formatTime(hour, minute))
                showDialog = false
            },
        )
    }
}

@Composable
internal fun DayScheduleRow(
    day: DayScheduleUi,
    onToggle: () -> Unit,
    onAddSlot: () -> Unit,
    onRemoveSlot: (String) -> Unit,
    onUpdateSlotStart: (String, String) -> Unit,
    onUpdateSlotEnd: (String, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = day.label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = day.enabled, onCheckedChange = { onToggle() })
        }

        if (day.enabled) {
            day.slots.forEach { slot ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeButton(label = "Inicio", time = slot.horaInicio) {
                        onUpdateSlotStart(slot.id, it)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("a")
                    Spacer(modifier = Modifier.width(8.dp))
                    TimeButton(label = "Fin", time = slot.horaFin) {
                        onUpdateSlotEnd(slot.id, it)
                    }
                    if (day.slots.size > 1) {
                        IconButton(onClick = { onRemoveSlot(slot.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar turno")
                        }
                    }
                }
            }

            TextButton(onClick = onAddSlot) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.height(16.dp).width(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar turno partido")
            }
        } else {
            Text(
                text = "Cerrado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private const val SCHEDULE_STEP = 4

/** Devuelve el error de cada día (si lo tiene). Las horas son "HH:mm", así que comparan bien como texto. */
private fun scheduleErrors(schedule: List<DayScheduleUi>): Map<Int, String> {
    val errors = mutableMapOf<Int, String>()
    schedule.filter { it.enabled }.forEach { day ->
        if (day.slots.any { it.horaFin <= it.horaInicio }) {
            errors[day.diaSemana] = "La hora de cierre debe ser posterior a la de apertura"
            return@forEach
        }
        val sorted = day.slots.sortedBy { it.horaInicio }
        if (sorted.zipWithNext().any { (a, b) -> b.horaInicio < a.horaFin }) {
            errors[day.diaSemana] = "Los turnos de este día no pueden superponerse"
        }
    }
    return errors
}

@Composable
fun BusinessScheduleScreen(
    schedule: List<DayScheduleUi>,
    onToggleDay: (Int) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, String) -> Unit,
    onUpdateSlotStart: (Int, String, String) -> Unit,
    onUpdateSlotEnd: (Int, String, String) -> Unit,
    onCopyMondayToAll: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val primary = colorResource(R.color.primary)
    val secondary = colorResource(R.color.secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    val errors = scheduleErrors(schedule)
    val openDays = schedule.count { it.enabled && it.slots.isNotEmpty() }
    val canContinue = errors.isEmpty() && openDays > 0

    Scaffold(containerColor = colorResource(R.color.splash_background)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OnboardingTopBar(currentStep = SCHEDULE_STEP, onBack = onBack)
            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = "¿Cuáles son tus horarios de atención?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configurá los días y horarios en los que atendés a los vecinos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colorResource(R.color.surface_card),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Horarios de atención",
                                style = MaterialTheme.typography.labelLarge,
                                color = textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(shape = CircleShape, color = secondary.copy(alpha = 0.12f)) {
                                Text(
                                    text = if (openDays == 1) "1 día abierto" else "$openDays días abierto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        TextButton(
                            onClick = onCopyMondayToAll,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "Copiar lunes a todos",
                                style = MaterialTheme.typography.labelMedium,
                                color = primary,
                            )
                        }

                        schedule.forEach { day ->
                            HorizontalDivider(color = colorResource(R.color.neutral_inactive).copy(alpha = 0.25f))
                            ScheduleDayItem(
                                day = day,
                                error = errors[day.diaSemana],
                                onToggle = { onToggleDay(day.diaSemana) },
                                onAddSlot = { onAddSlot(day.diaSemana) },
                                onRemoveSlot = { slotId -> onRemoveSlot(day.diaSemana, slotId) },
                                onUpdateSlotStart = { slotId, time -> onUpdateSlotStart(day.diaSemana, slotId, time) },
                                onUpdateSlotEnd = { slotId, time -> onUpdateSlotEnd(day.diaSemana, slotId, time) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(R.color.tertiary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Podrás pausar o ajustar estos horarios en cualquier momento desde tu panel de comercio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            OnboardingPrimaryButton(text = "Siguiente →", onClick = onNext, enabled = canContinue)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDayItem(
    day: DayScheduleUi,
    error: String?,
    onToggle: () -> Unit,
    onAddSlot: () -> Unit,
    onRemoveSlot: (String) -> Unit,
    onUpdateSlotStart: (String, String) -> Unit,
    onUpdateSlotEnd: (String, String) -> Unit,
) {
    val primary = colorResource(R.color.primary)
    val inactive = colorResource(R.color.neutral_inactive)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = day.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_primary),
            )
            if (!day.enabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = inactive.copy(alpha = 0.2f)) {
                    Text(
                        text = "Cerrado",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(R.color.text_secondary),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = day.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(R.color.on_primary),
                    checkedTrackColor = colorResource(R.color.secondary),
                    uncheckedThumbColor = colorResource(R.color.on_primary),
                    uncheckedTrackColor = inactive.copy(alpha = 0.5f),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }

        if (day.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                day.slots.forEach { slot ->
                    TimeSlotChip(
                        slot = slot,
                        showRemove = day.slots.size > 1,
                        hasError = error != null,
                        onStartChange = { onUpdateSlotStart(slot.id, it) },
                        onEndChange = { onUpdateSlotEnd(slot.id, it) },
                        onRemove = { onRemoveSlot(slot.id) },
                    )
                }
                Text(
                    text = "+ Agregar turno",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    modifier = Modifier.clickable(onClick = onAddSlot).padding(vertical = 6.dp),
                )
            }
            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.error),
                )
            }
        }
    }
}

@Composable
private fun TimeSlotChip(
    slot: TimeSlotUi,
    showRemove: Boolean,
    hasError: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val primary = colorResource(R.color.primary)
    val borderColor = if (hasError) colorResource(R.color.error) else primary.copy(alpha = 0.5f)
    // 0 = cerrado, 1 = eligiendo apertura, 2 = eligiendo cierre
    var step by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(primary.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable { step = 1 }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (slot.horaInicio < "14:00") Icons.Filled.WbSunny else Icons.Filled.WbTwilight,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${slot.horaInicio} - ${slot.horaFin}",
            style = MaterialTheme.typography.labelMedium,
            color = colorResource(R.color.text_primary),
        )
        if (showRemove) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Quitar turno",
                tint = colorResource(R.color.text_secondary),
                modifier = Modifier.size(14.dp).clickable(onClick = onRemove),
            )
        }
    }

    if (step == 1) {
        val (h, m) = parseTime(slot.horaInicio)
        TimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismiss = { step = 0 },
            onConfirm = { hour, minute ->
                onStartChange(formatTime(hour, minute))
                step = 2
            },
        )
    }
    if (step == 2) {
        val (h, m) = parseTime(slot.horaFin)
        TimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismiss = { step = 0 },
            onConfirm = { hour, minute ->
                onEndChange(formatTime(hour, minute))
                step = 0
            },
        )
    }
}