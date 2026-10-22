package com.barrioahorro.app.ui.cuponera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.cuponera.RaffleCommitmentDto
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.FieldBackground
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.StepperBorder
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.WarmGray
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

private val RaffleCardShape = RoundedCornerShape(20.dp)
private val RAFFLE_SUGGESTIONS = listOf("Canasta artesanal", "Almuerzo para 2", "Vale de compra")
private val RAFFLE_MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

@Composable
fun RaffleRatifyScreen(
    onBack: () -> Unit,
    onRatified: () -> Unit,
    viewModel: RaffleRatifyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { onRatified() }
    }

    Scaffold(
        containerColor = Cream,
        modifier = Modifier.imePadding(),
        topBar = { ContributionTopBar(title = "Ratificar premio", onBack = onBack) },
        bottomBar = {
            if (!state.loading && state.loadError == null) {
                RaffleBottomAction(
                    state = state,
                    onRatify = viewModel::onRequestConfirm,
                    onContinue = viewModel::onContinue,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Primary) }

            state.loadError != null -> ContributionErrorState(
                message = state.loadError.orEmpty(),
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val ratified = state.ratified
                RaffleHeader(ratified = ratified != null)
                if (ratified != null) {
                    RaffleRatifiedContent(ratified)
                } else {
                    RafflePendingContent(state, viewModel)
                }
            }
        }
    }

    if (state.showConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirm,
            containerColor = CardWhite,
            title = { Text("¿Confirmás tu premio?", color = InkBlack, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Vas a comprometer \"${state.name.trim()}\" para el próximo sorteo. " +
                            "Una vez ratificado no vas a poder cambiarlo ni retirarlo.",
                    color = Ink,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::submit) {
                    Text("Confirmar", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissConfirm) {
                    Text("Revisar", color = WarmGray)
                }
            },
        )
    }
}

// Header

@Composable
private fun RaffleHeader(ratified: Boolean) {
    Surface(shape = RaffleCardShape, color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = OnPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (ratified) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        "COMPROMISO COMUNITARIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ratified) Secondary else Primary,
                    )
                }
                Text(
                    "Premio para el sorteo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                )
                Text(
                    "Comprometé un premio de mayor valor para los vecinos y fortalecé la confianza local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
    }
}

// Estado PENDIENTE

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RafflePendingContent(state: RaffleRatifyUiState, viewModel: RaffleRatifyViewModel) {
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = FieldBackground,
        unfocusedContainerColor = FieldBackground,
        focusedIndicatorColor = Primary,
        unfocusedIndicatorColor = StepperBorder,
        focusedLabelColor = Primary,
        cursorColor = Primary,
    )

    Surface(shape = RaffleCardShape, color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Premio *") },
                placeholder = { Text("Ej.: Canasta de panadería") },
                singleLine = true,
                colors = fieldColors,
                supportingText = {
                    Text(
                        "${state.name.length}/$RAFFLE_NAME_MAX_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Describí algo atractivo y claro que motive a sumar puntos en tu tienda.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )

            Text(
                "SUGERENCIAS POPULARES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MutedLabel,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RAFFLE_SUGGESTIONS.forEach { suggestion ->
                    Surface(
                        onClick = { viewModel.onNameChange(suggestion) },
                        shape = RoundedCornerShape(50),
                        color = SurfaceSecondary,
                        border = BorderStroke(1.dp, StepperBorder),
                    ) {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.valueText,
                onValueChange = viewModel::onValueChange,
                label = { Text("Valor en tu comercio *") },
                prefix = { Text("$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción *") },
                placeholder = { Text("Qué incluye, tamaño, marca…") },
                minLines = 3,
                maxLines = 5,
                colors = fieldColors,
                supportingText = {
                    Text(
                        "${state.description.length}/$RAFFLE_DESCRIPTION_MAX_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.target?.let { target ->
        RaffleValidityCard(target, active = false)
        if (state.canRatify) {
            formatDeadline(target.ratificationDeadline)?.let { deadline ->
                Text(
                    "Tenés tiempo hasta el $deadline para ratificarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
    }

    RaffleInfoNote(
        icon = Icons.Filled.Lock,
        text = "Una vez ratificado, no podés cambiar ni retirar el premio antes del sorteo " +
                "para asegurar la transparencia del evento.",
    )
}

// Estado RATIFICADO

@Composable
private fun RaffleRatifiedContent(commitment: RaffleCommitmentDto) {
    Surface(shape = RaffleCardShape, color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(SecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = Secondary)
            }
            Text(
                "¡Todo listo!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                modifier = Modifier.weight(1f),
            )
            StatusPill(text = "✓ Confirmado", container = SecondaryContainer, content = Secondary)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Premio comprometido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.Lock, contentDescription = null, tint = WarmGray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("No editable", style = MaterialTheme.typography.labelMedium, color = WarmGray)
    }

    Surface(
        shape = RaffleCardShape,
        color = SurfaceSecondary,
        border = BorderStroke(1.dp, StepperBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PREMIO RATIFICADO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MutedLabel,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(text = "Bloqueado", container = PrimaryContainer, content = Primary)
            }
            Text(
                commitment.prizeName.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
            )
            commitment.prizeDescription?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                commitment.prizeValue?.let {
                    Text(
                        "Valor estimado: ${formatMoney(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                } ?: Spacer(Modifier.weight(1f))
                Text(
                    "Folio #SO-${commitment.cycleStart.take(4)}-${commitment.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
    }

    RaffleValidityCard(commitment, active = true)

    RaffleInfoNote(
        icon = Icons.Outlined.Info,
        title = "POLÍTICA DE TRANSPARENCIA",
        text = "El premio queda bloqueado hasta el sorteo. Los vecinos van a ver el premio " +
                "comprometido en la vidriera digital del barrio.",
    )
}

// Componentes

@Composable
private fun RaffleValidityCard(commitment: RaffleCommitmentDto, active: Boolean) {
    Surface(shape = RaffleCardShape, color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Vigencia del compromiso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                    )
                    Text(
                        "Período oficial de participación vecinal",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }
                if (active) {
                    StatusPill(text = "Activo", container = SecondaryContainer, content = Secondary)
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceSecondary, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (active) "PERÍODO DE VALIDEZ" else "INICIO Y FIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MutedLabel,
                    )
                    Text(
                        "Del ${formatDate(commitment.cycleStart)} al ${formatDate(commitment.cycleEnd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    if (active) {
                        exposureDays(commitment)?.let { days ->
                            Text(
                                "$days días de exposición en vidriera digital barrial",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RaffleInfoNote(icon: ImageVector, text: String, title: String? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSecondary,
        border = BorderStroke(1.dp, StepperBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                title?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    }
}

@Composable
private fun RaffleBottomAction(
    state: RaffleRatifyUiState,
    onRatify: () -> Unit,
    onContinue: () -> Unit,
) {
    val ratified = state.ratified != null
    Surface(color = Cream, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.error?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Button(
                onClick = if (ratified) onContinue else onRatify,
                enabled = ratified || state.canSubmit,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        if (ratified) "Continuar" else "Ratificar premio",
                        fontWeight = FontWeight.Bold,
                    )
                    if (!ratified) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (!ratified && !state.submitting) {
                val hint = when {
                    !state.canRatify -> "Todavía no es momento de ratificar el próximo sorteo."
                    !state.canSubmit -> "Completá el premio para continuar."
                    else -> null
                }
                hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WarmGray) }
            }
        }
    }
}

// Formateo

/** "2026-10-12" → "12 de octubre de 2026" */
private fun formatDate(iso: String): String = runCatching {
    val date = LocalDate.parse(iso.take(10))
    "${date.dayOfMonth} de ${RAFFLE_MONTHS[date.monthValue - 1]} de ${date.year}"
}.getOrDefault(iso)

/** Deadline en OffsetDateTime → "21 de octubre" en la zona del teléfono. */
private fun formatDeadline(iso: String): String? = runCatching {
    val date = OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .minusMinutes(1) // un deadline a las 00:00 se muestra como el día anterior
    "${date.dayOfMonth} de ${RAFFLE_MONTHS[date.monthValue - 1]}"
}.getOrNull()

private fun exposureDays(commitment: RaffleCommitmentDto): Long? = runCatching {
    ChronoUnit.DAYS.between(
        LocalDate.parse(commitment.cycleStart.take(10)),
        LocalDate.parse(commitment.cycleEnd.take(10)),
    )
}.getOrNull()

/** 25000.0 → "$25.000" */
private fun formatMoney(value: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("es", "AR")).apply {
        maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = maximumFractionDigits
    }
    return "$" + format.format(value)
}