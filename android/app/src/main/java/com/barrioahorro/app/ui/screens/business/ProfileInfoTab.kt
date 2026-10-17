package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.components.profile.StatusChip
import com.barrioahorro.app.ui.theme.HighlightOrange
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.WarmGray
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.barrioahorro.app.ui.theme.OnPrimary

@Composable
internal fun ProfileInfoTab(state: BusinessProfileUiState,
                            onLogout: () -> Unit,
                            isLoggingOut: Boolean,) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileCard {
            SectionHeader(icon = Icons.Outlined.Storefront, title = "Sobre ${state.businessName}")
            Text(
                text = state.descripcion.ifBlank { "Todavía no cargaste una descripción." },
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }

        ProfileCard {
            SectionHeader(icon = Icons.Outlined.Schedule, title = "Horarios de atención") {
                if (state.isOpenToday) {
                    StatusChip(
                        text = "Abierto hoy",
                        containerColor = SecondaryContainer,
                        contentColor = Secondary,
                        dotColor = Secondary,
                    )
                } else {
                    StatusChip(text = "Cerrado hoy", containerColor = SurfaceMuted, contentColor = WarmGray)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.schedule.forEach { day ->
                    ScheduleRow(
                        label = day.label,
                        slotsText = if (day.enabled && day.slots.isNotEmpty()) {
                            day.slots.joinToString(" · ") { "${it.horaInicio.take(5)} a ${it.horaFin.take(5)}" }
                        } else {
                            null
                        },
                        isToday = day.diaSemana == state.todayDiaSemana,
                    )
                }
            }
        }

        ProfileCard {
            SectionHeader(icon = Icons.Outlined.LocationOn, title = "Dirección")
            Text(
                text = state.direccion.ifBlank { "Sin dirección cargada" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            val latitud = state.latitud
            val longitud = state.longitud
            if (latitud != null && longitud != null) {
                BusinessAddressMap(
                    latitud = latitud,
                    longitud = longitud,
                    isFetchingLocation = false,
                    onUseCurrentLocation = {},
                    showLocationPill = false,
                )
            }
        }
        LogoutButton(onClick = onLogout, isLoading = isLoggingOut)
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun ScheduleRow(label: String, slotsText: String?, isToday: Boolean) {
    val rowModifier = if (isToday) {
        Modifier.background(HighlightOrange, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isToday) {
                Box(modifier = Modifier.size(6.dp).background(Primary, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = if (isToday) "$label (Hoy)" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = Ink,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = slotsText ?: "Cerrado por descanso",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (slotsText == null) FontStyle.Italic else FontStyle.Normal,
            color = if (slotsText == null) WarmGray else Ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
@Composable
private fun LogoutButton(onClick: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary,
            disabledContainerColor = Primary,
            disabledContentColor = OnPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(52.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}