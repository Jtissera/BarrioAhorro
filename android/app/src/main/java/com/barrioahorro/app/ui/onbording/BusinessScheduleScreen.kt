package com.barrioahorro.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
private fun DayScheduleRow(
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

@Composable
fun BusinessScheduleScreen(
    descripcion: String,
    schedule: List<DayScheduleUi>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDescripcionChange: (String) -> Unit,
    onToggleDay: (Int) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, String) -> Unit,
    onUpdateSlotStart: (Int, String, String) -> Unit,
    onUpdateSlotEnd: (Int, String, String) -> Unit,
    onCopyMondayToAll: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Paso 4 de 4", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Últimos detalles", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cerrá tu perfil con una breve descripción y tus horarios de atención.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(text = "Descripción", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = onDescripcionChange,
                    placeholder = { Text("Contales a tus vecinos qué los hace especial...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Horarios de atención", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onCopyMondayToAll) {
                        Text("Copiar lunes a todos")
                    }
                }
            }

            items(schedule, key = { it.diaSemana }) { day ->
                DayScheduleRow(
                    day = day,
                    onToggle = { onToggleDay(day.diaSemana) },
                    onAddSlot = { onAddSlot(day.diaSemana) },
                    onRemoveSlot = { slotId -> onRemoveSlot(day.diaSemana, slotId) },
                    onUpdateSlotStart = { slotId, time -> onUpdateSlotStart(day.diaSemana, slotId, time) },
                    onUpdateSlotEnd = { slotId, time -> onUpdateSlotEnd(day.diaSemana, slotId, time) },
                )
            }

            errorMessage?.let {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, enabled = !isSubmitting, modifier = Modifier.width(56.dp)) {
                Text("<")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onFinish,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                } else {
                    Text("Finalizar y ver mi panel")
                }
            }
        }
    }
}