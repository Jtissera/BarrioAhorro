package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceDay
import com.barrioahorro.app.ui.theme.SurfaceDayClosed
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.ui.components.profile.FormFieldHeader
import com.barrioahorro.app.ui.components.profile.StatusChip
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import com.barrioahorro.app.ui.screens.onboarding.TimeSlotUi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.barrioahorro.app.ui.theme.ErrorRed

/** "Horarios de atención" field: header with "Copiar a todos" plus the white card listing every day. */
@Composable
fun BusinessScheduleCard(
    schedule: List<DayScheduleUi>,
    onToggleDay: (Int) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, String) -> Unit,
    onUpdateSlotStart: (Int, String, String) -> Unit,
    onUpdateSlotEnd: (Int, String, String) -> Unit,
    onCopyMondayToAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = Primary
    val errors = scheduleErrors(schedule)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormFieldHeader(label = "Horarios de atención") {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCopyMondayToAll)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "Copiar a todos", style = MaterialTheme.typography.labelMedium, color = primary)
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                schedule.forEach { day ->
                    ScheduleDayCard(
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDayCard(
    day: DayScheduleUi,
    error: String?,
    onToggle: () -> Unit,
    onAddSlot: () -> Unit,
    onRemoveSlot: (String) -> Unit,
    onUpdateSlotStart: (String, String) -> Unit,
    onUpdateSlotEnd: (String, String) -> Unit,
) {
    val primary = Primary
    val textPrimary = Ink
    val shape = RoundedCornerShape(12.dp)

    if (!day.enabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.8f)
                .background(SurfaceDayClosed.copy(alpha = 0.6f), shape)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DayTitle(label = day.label)
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(
                text = "Cerrado",
                containerColor = SurfaceMuted,
                contentColor = WarmGray,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Activar día",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDay.copy(alpha = 0.4f), shape)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DayTitle(label = day.label)
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(
                text = "Abierto",
                containerColor = SecondaryContainer,
                contentColor = Secondary,
                dotColor = Secondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = true,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = textPrimary,
                    checkedBorderColor = Color.Transparent,
                ),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddSlot)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        error?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
        }
    }
}

@Composable
private fun DayTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = Ink,
    )
}

/** Time range chip. Tapping it asks for the opening time first and then the closing time. */
@Composable
private fun TimeSlotChip(
    slot: TimeSlotUi,
    showRemove: Boolean,
    hasError: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    // 0 = idle, 1 = picking opening time, 2 = picking closing time
    var step by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(Cream)
            .border(BorderStroke(1.dp, if (hasError) ErrorRed else Color.Transparent), shape)
            .clickable { step = 1 }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${slot.horaInicio} – ${slot.horaFin}",
            style = MaterialTheme.typography.labelMedium,
            color = Ink,
        )
        if (showRemove) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Quitar turno",
                tint = Primary,
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemove),
            )
        }
    }

    if (step == 1) {
        val (hour, minute) = parseTime(slot.horaInicio)
        SlotTimePickerDialog(
            title = "Hora de apertura",
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { step = 0 },
            onConfirm = { h, m ->
                onStartChange(formatTime(h, m))
                step = 2
            },
        )
    }
    if (step == 2) {
        val (hour, minute) = parseTime(slot.horaFin)
        SlotTimePickerDialog(
            title = "Hora de cierre",
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { step = 0 },
            onConfirm = { h, m ->
                onEndChange(formatTime(h, m))
                step = 0
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    val primary = Primary
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar", color = primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = WarmGray) }
        },
        text = { TimePicker(state = state) },
    )
}

private fun parseTime(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 9) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

@Preview(showBackground = true, backgroundColor = 0xFFFBF6E6)
@Composable
private fun BusinessScheduleCardPreview() {
    BusinessScheduleCard(
        schedule = listOf(
            DayScheduleUi(1, "Lunes", true, listOf(TimeSlotUi("a", "08:30", "13:00"), TimeSlotUi("b", "16:30", "20:00"))),
            DayScheduleUi(6, "Sábado", true, listOf(TimeSlotUi("c", "08:30", "13:30"))),
            DayScheduleUi(7, "Domingo", false, emptyList()),
        ),
        onToggleDay = {},
        onAddSlot = {},
        onRemoveSlot = { _, _ -> },
        onUpdateSlotStart = { _, _, _ -> },
        onUpdateSlotEnd = { _, _, _ -> },
        onCopyMondayToAll = {},
        modifier = Modifier.padding(16.dp),
    )
}
