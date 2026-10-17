package com.barrioahorro.app.ui.sorteo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.sorteo.SorteoCommitmentDto
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.StepperBorder
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.WarmGray
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

private val CardShape = RoundedCornerShape(20.dp)
private val SUGGESTIONS = listOf("Canasta artesanal", "Almuerzo para 2", "Vale de compra")
private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

@Composable
fun SorteoRatificationScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: SorteoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is SorteoEvent.Ratified) onContinue() }
    }

    val commitment = state.commitment

    Scaffold(
        containerColor = Cream,
        topBar = { TopBar(onBack) },
        bottomBar = {
            if (commitment != null) {
                BottomAction(
                    commitment = commitment,
                    canRatify = state.canRatify,
                    submitting = state.submitting,
                    onRatify = viewModel::onRatifyClick,
                    onContinue = viewModel::onContinue,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)

                commitment == null -> LoadError(
                    message = state.loadError ?: "No pudimos cargar tu compromiso.",
                    onRetry = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Header(ratified = commitment.estado == "RATIFICADO")

                    when {
                        commitment.estado == "RATIFICADO" -> RatifiedContent(commitment)
                        commitment.puedeRatificar -> PendingContent(
                            commitment = commitment,
                            premio = state.premio,
                            onPremioChange = viewModel::onPremioChange,
                            onSuggestion = viewModel::onSuggestion,
                        )
                        else -> ExpiredContent()
                    }

                    state.submitError?.let {
                        Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (state.showConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirm,
            containerColor = CardWhite,
            title = { Text("¿Confirmás tu premio?", color = Ink) },
            text = {
                Text(
                    "\"${state.premio.trim()}\"\n\nUna vez ratificado no vas a poder cambiarlo ni retirarlo hasta que se realice el sorteo.",
                    color = WarmGray,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmRatify,
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary),
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::onDismissConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = WarmGray),
                ) { Text("Revisar") }
            },
        )
    }
}

// estructura

@Composable
private fun TopBar(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cream)
            .statusBarsPadding(),
    ) {
        ToldoConFranjas(altColor = Cream)
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink)
            }
            Text("Ratificar premio", style = MaterialTheme.typography.titleMedium, color = Ink)
        }
    }
}

@Composable
private fun Header(ratified: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(14.dp), color = Primary, modifier = Modifier.size(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = OnPrimary)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ratified) {
                    Icon(Icons.Filled.Verified, null, tint = Secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    "COMPROMISO COMUNITARIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ratified) Secondary else Primary,
                )
            }
            Text("Premio para el sorteo", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(
                "Comprometé un premio de mayor valor para los vecinos y fortalecé la confianza local.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun BottomAction(
    commitment: SorteoCommitmentDto,
    canRatify: Boolean,
    submitting: Boolean,
    onRatify: () -> Unit,
    onContinue: () -> Unit,
) {
    val ratified = commitment.estado == "RATIFICADO"
    if (!ratified && !commitment.puedeRatificar) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = if (ratified) onContinue else onRatify,
            enabled = ratified || canRatify,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = OnPrimary,
                disabledContainerColor = MutedLabel.copy(alpha = 0.5f),
                disabledContentColor = OnPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (submitting) {
                CircularProgressIndicator(Modifier.size(22.dp), color = OnPrimary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ratified) "Continuar" else "Ratificar premio", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (!ratified && !canRatify) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = WarmGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Completá el premio para continuar.", style = MaterialTheme.typography.bodySmall, color = WarmGray)
            }
        }
    }
}

// estados

@Composable
private fun PendingContent(
    commitment: SorteoCommitmentDto,
    premio: String,
    onPremioChange: (String) -> Unit,
    onSuggestion: (String) -> Unit,
) {
    // Campo del premio
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Premio", style = MaterialTheme.typography.labelLarge, color = Ink)
            Text(" *", style = MaterialTheme.typography.labelLarge, color = Primary)
            Spacer(Modifier.weight(1f))
            Text("${premio.length}/$MAX_PREMIO_LENGTH", style = MaterialTheme.typography.labelSmall, color = WarmGray)
        }
        TextField(
            value = premio,
            onValueChange = onPremioChange,
            singleLine = true,
            placeholder = { Text("Ej.: Almuerzo para dos", color = MutedLabel) },
            leadingIcon = { Icon(Icons.Outlined.CardGiftcard, null, tint = WarmGray) },
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Primary,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Describí algo atractivo y claro que motive a sumar puntos en tu tienda.",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
        )
    }

    // Sugerencias
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SUGERENCIAS POPULARES", style = MaterialTheme.typography.labelSmall, color = WarmGray)
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SUGGESTIONS.forEach { s ->
                Surface(
                    onClick = { onSuggestion(s) },
                    shape = RoundedCornerShape(50),
                    color = SurfaceSecondary,
                    border = BorderStroke(1.dp, StepperBorder),
                ) {
                    Text(
                        s,
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }

    ValidityCard(commitment, active = false)

    Text(
        "Tenés tiempo hasta el ${formatDeadline(commitment.fechaLimiteRatificacion)} para ratificarlo.",
        style = MaterialTheme.typography.bodySmall,
        color = WarmGray,
    )

    InfoNote(
        icon = Icons.Outlined.Lock,
        title = null,
        text = "Una vez ratificado, no podés cambiar ni retirar el premio antes del sorteo para asegurar la transparencia del evento.",
    )
}

@Composable
private fun RatifiedContent(commitment: SorteoCommitmentDto) {
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = CardWhite), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = SecondaryContainer, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MilitaryTech, null, tint = Secondary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¡Todo listo!", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.width(8.dp))
                    StatusPill("Confirmado")
                }
                Text(
                    "Tu premio ya fue confirmado para el próximo sorteo comunitario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
        }
    }

    // Premio comprometido
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Premio comprometido", style = MaterialTheme.typography.labelLarge, color = Ink, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Lock, null, tint = WarmGray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("No editable", style = MaterialTheme.typography.labelSmall, color = WarmGray)
    }
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = SurfaceSecondary), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = WarmGray)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PREMIO RATIFICADO", style = MaterialTheme.typography.labelSmall, color = Primary)
                    Text(
                        commitment.premioDescripcion.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                }
            }
            Text(
                "Folio #SO-${commitment.cicloInicio.take(4)}-${commitment.id}",
                style = MaterialTheme.typography.labelMedium,
                color = WarmGray,
            )
        }
    }

    ValidityCard(commitment, active = true)

    InfoNote(
        icon = Icons.Outlined.Info,
        title = "POLÍTICA DE TRANSPARENCIA",
        text = "Una vez ratificado, no podés cambiar ni retirar el premio antes del sorteo.",
    )
}

@Composable
private fun ExpiredContent() {
    InfoNote(
        icon = Icons.Outlined.Lock,
        title = "PLAZO VENCIDO",
        text = "El plazo para ratificar el premio de este ciclo ya venció. Tu cuenta tiene el acceso restringido hasta el próximo ciclo.",
    )
}

// componentes

@Composable
private fun ValidityCard(commitment: SorteoCommitmentDto, active: Boolean) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = PrimaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = Primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vigencia del compromiso", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text("Período oficial de participación vecinal", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
                if (active) StatusPill("Activo")
            }
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceSecondary, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("PERÍODO DE VALIDEZ", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                    Text(
                        "Del ${formatDate(commitment.cicloInicio)} al ${formatDate(commitment.cicloFin)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = SecondaryContainer) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoNote(icon: ImageVector, title: String?, text: String) {
    Surface(shape = CardShape, color = SurfaceSecondary, border = BorderStroke(1.dp, StepperBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = WarmGray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                title?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Ink) }
                Text(text, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
            }
        }
    }
}

@Composable
private fun LoadError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = Ink, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Reintentar")
        }
    }
}

// parseo fechas
private fun formatDate(iso: String): String =
    runCatching {
        val d = LocalDate.parse(iso)
        "${d.dayOfMonth} de ${MONTHS[d.monthValue - 1]} de ${d.year}"
    }.getOrDefault(iso)

private fun formatDeadline(iso: String): String =
    runCatching {
        val d = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).minusMinutes(1).toLocalDate()
        "${d.dayOfMonth} de ${MONTHS[d.monthValue - 1]}"
    }.getOrDefault(iso)

