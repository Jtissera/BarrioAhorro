package com.barrioahorro.app.ui.cuponera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.cuponera.CommitmentOverviewDto
import com.barrioahorro.app.data.cuponera.CuponeraCycleDto
import com.barrioahorro.app.data.cuponera.CuponeraProductDto
import com.barrioahorro.app.data.cuponera.RaffleCycleDto
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.WarmGray
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.EmojiEvents
import com.barrioahorro.app.data.cuponera.RaffleCommitmentDto

private val SectionShape = RoundedCornerShape(20.dp)

/** Contenido de la pestaña Cuponera del comercio. La navegación se conecta desde afuera. */
@Composable
fun MyContributionScreen(
    onScanQr: () -> Unit,
    onRatifyCuponera: () -> Unit,
    onRatifyRaffle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyContributionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Al entrar a la pestaña o volver de escanear / ratificar, se actualiza el estado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        ContributionTopBar(title = "Mi aporte")

        val overview = state.overview
        when {
            overview != null -> ContributionContent(
                overview = overview,
                onScanQr = onScanQr,
                onRatifyCuponera = onRatifyCuponera,
                onRatifyRaffle = onRatifyRaffle,
                modifier = Modifier.weight(1f),
            )

            state.loading -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }

            else -> ContributionErrorState(
                message = state.error ?: "No se pudo cargar tu aporte.",
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContributionContent(
    overview: CommitmentOverviewDto,
    onScanQr: () -> Unit,
    onRatifyCuponera: () -> Unit,
    onRatifyRaffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alerts = ratifyAlerts(overview)
    val products = overview.cuponera.current?.productos.orEmpty()

    Column(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Estado de mi aporte",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                    )
                    Text(
                        text = "Seguimiento de tus productos de cuponera y tu premio del sorteo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGray,
                    )
                }
            }

            if (overview.restrictedAccess || alerts.isNotEmpty()) {
                item {
                    AlertBanner(
                        messages = alerts.ifEmpty { listOf("Ratificá tu aporte para volver a publicar ofertas.") },
                        severe = overview.restrictedAccess,
                    )
                }
            }

            item { SectionLabel("CUPONERA") }
            item { CuponeraPeriodCard(overview.cuponera) }

            if (products.isNotEmpty()) {
                item { SectionLabel("PRODUCTOS COMPROMETIDOS") }
                items(products, key = { it.id }) { product -> ProductStockCard(product) }
            }

            item { SectionLabel("SORTEO") }
            item { RaffleCard(overview.raffle) }
        }

        BottomActions(
            overview = overview,
            onScanQr = onScanQr,
            onRatifyCuponera = onRatifyCuponera,
            onRatifyRaffle = onRatifyRaffle,
        )
    }
}

// ---- Avisos de ratificación ----------------------------------------------------

private fun ratifyAlerts(overview: CommitmentOverviewDto): List<String> = buildList {
    val restricted = overview.restrictedAccess
    if (overview.cuponera.canRatify) {
        add(
            alertFor(
                what = "tus productos de la cuponera",
                currentEnd = overview.cuponera.current?.periodoFin,
                deadline = overview.cuponera.next?.fechaLimiteRatificacion,
                restricted = restricted,
            ),
        )
    }
    if (overview.raffle.canRatify) {
        add(
            alertFor(
                what = "el premio del sorteo",
                currentEnd = overview.raffle.current?.cycleEnd,
                deadline = overview.raffle.next?.ratificationDeadline,
                restricted = restricted,
            ),
        )
    }
}

private fun alertFor(what: String, currentEnd: String?, deadline: String?, restricted: Boolean): String {
    val goal = if (restricted) "para recuperar el acceso" else "para no perder el acceso"
    val end = parseIsoDate(currentEnd)
    val limit = formatLongDate(deadline).takeIf { it.isNotEmpty() }?.let { " antes del $it" }.orEmpty()

    if (end != null) {
        val days = ChronoUnit.DAYS.between(todayInBusinessZone(), end)
        val whenText = when {
            days <= 0L -> "hoy"
            days == 1L -> "mañana"
            else -> "en $days días"
        }
        return "El período vigente termina $whenText. Ratificá $what$limit $goal."
    }

    return "Ratificá $what$limit $goal."
}

@Composable
private fun AlertBanner(messages: List<String>, severe: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (severe) PrimaryContainer else BadgeYellow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (severe) "Tu acceso está limitado" else "Ratificación próxima",
                fontWeight = FontWeight.Bold,
                color = if (severe) Primary else BadgeYellowText,
            )
            messages.forEach { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    }
}

// ---- Cuponera --------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = WarmGray,
    )
}

private fun cycleProgress(startIso: String, endIso: String): Triple<Long, Long, Float>? {
    val start = parseIsoDate(startIso) ?: return null
    val end = parseIsoDate(endIso) ?: return null
    val total = ChronoUnit.DAYS.between(start, end) + 1
    if (total < 1) return null
    val day = (ChronoUnit.DAYS.between(start, todayInBusinessZone()) + 1).coerceIn(1, total)
    return Triple(day, total, day.toFloat() / total)
}

@Composable
private fun CuponeraPeriodCard(cycle: CuponeraCycleDto) {
    val current = cycle.current
    val next = cycle.next
    val nextRatified = next?.takeIf { it.estado == "RATIFICADO" }

    Card(shape = SectionShape, colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (current == null) {
                Text(
                    text = "Todavía no tenés un período vigente ratificado.",
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Período vigente",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill("Vigente", Secondary, OnPrimary)
                }

                cycleProgress(current.periodoInicio, current.periodoFin)?.let { (day, total, fraction) ->
                    Text(
                        text = "Día $day de $total del ciclo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                    )
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Primary,
                        trackColor = SurfaceSecondary,
                    )
                }

                Spacer(Modifier.height(2.dp))
                HorizontalDivider(color = SurfaceSecondary, thickness = 1.dp)
                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅  Vence el:",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                        modifier = Modifier.width(135.dp),
                    )
                    Text(
                        text = formatLongDate(current.periodoFin),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                }
            }

            if (nextRatified != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SecondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Text(
                        text = "✓ Próximo período ratificado (${formatLongDate(nextRatified.periodoInicio)} al ${formatLongDate(nextRatified.periodoFin)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductStockCard(product: CuponeraProductDto) {
    val total = maxOf(1, product.unidadesDisponibles)
    val left = (product.unidadesDisponibles - product.unidadesCanjeadas).coerceAtLeast(0)

    Card(shape = SectionShape, colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CuponeraProductPhoto(
                    nombre = product.nombreProducto,
                    fotoUrl = product.fotoUrl,
                    fotoSubiendo = false,
                    editable = false,
                    onPhotoCaptured = {},
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(product.nombreProducto, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    product.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Canjeados ${product.unidadesCanjeadas} · Disponibles $left",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill("${product.costoPuntos} pts", PrimaryContainer, Primary)
            }
            LinearProgressIndicator(
                progress = { (product.unidadesCanjeadas.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = SurfaceSecondary,
            )
        }
    }
}

// ---- Sorteo ----------------------------------------------------------------------

@Composable
private fun RaffleCard(cycle: RaffleCycleDto) {
    val current = cycle.current
    val next = cycle.next
    val nextRatified = next?.takeIf { it.status == "RATIFICADO" }

    // Sin premio vigente ni próximo ratificado: cartel llamativo.
    if (current == null && nextRatified == null) {
        RafflePendingCard(next)
        return
    }

    Card(shape = SectionShape, colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Premio del sorteo",
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill("Ratificado", Secondary, OnPrimary)
                }

                Text(
                    text = current.prizeName ?: current.prizeDescription ?: "Sin nombre",
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                )
                formatPesos(current.prizeValue)?.let {
                    Text("Valor en tu comercio: $it", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
                if (current.prizeName != null) {
                    current.prizeDescription?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = InkBlack)
                    }
                }

                // Período del sorteo: ciclo de 4 meses, distinto al de la cuponera.
                Surface(shape = RoundedCornerShape(14.dp), color = SurfaceSecondary, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Período del sorteo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                modifier = Modifier.weight(1f),
                            )
                            StatusPill("Ciclo de 4 meses", PrimaryContainer, Primary)
                        }
                        Text(
                            text = "${formatLongDate(current.cycleStart)} al ${formatLongDate(current.cycleEnd)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack,
                        )
                        cycleProgress(current.cycleStart, current.cycleEnd)?.let { (day, total, fraction) ->
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Secondary,
                                trackColor = CardWhite,
                            )
                            Text(
                                text = "Día $day de $total del ciclo",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray,
                            )
                        }
                    }
                }
            }

            if (nextRatified != null) {
                Text(
                    text = "Próximo sorteo ratificado (${formatLongDate(nextRatified.cycleStart)} " +
                            "al ${formatLongDate(nextRatified.cycleEnd)}): " +
                            (nextRatified.prizeName ?: nextRatified.prizeDescription).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RafflePendingCard(next: RaffleCommitmentDto?) {
    Surface(
        shape = SectionShape,
        color = PrimaryContainer,
        border = BorderStroke(1.5.dp, Primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Falta tu premio del sorteo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                    )
                    Text(
                        text = "Todavía no ratificaste el premio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                }
                StatusPill("PENDIENTE", Primary, OnPrimary)
            }

            if (next != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Ciclo a ratificar (4 meses)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmGray,
                        )
                        Text(
                            text = "${formatLongDate(next.cycleStart)} al ${formatLongDate(next.cycleEnd)}",
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack,
                        )
                        formatLongDate(next.ratificationDeadline).takeIf { it.isNotEmpty() }?.let {
                            Text(
                                text = "Ratificá antes del $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Cargá el producto que van a poder ganar los vecinos para mantener tu acceso a la app.",
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
            )
        }
    }
}

// ---- Acciones --------------------------------------------------------------------

@Composable
private fun BottomActions(
    overview: CommitmentOverviewDto,
    onScanQr: () -> Unit,
    onRatifyCuponera: () -> Unit,
    onRatifyRaffle: () -> Unit,
) {
    Surface(color = Cream, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Escanear siempre está disponible: los vecinos con un QR emitido tienen que poder canjear.
            Button(
                onClick = onScanQr,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR", fontWeight = FontWeight.Bold)
            }

            if (overview.cuponera.canRatify) RatifyButton("Ratificar cuponera", onRatifyCuponera)
            if (overview.raffle.canRatify) RatifyButton("Ratificar sorteo", onRatifyRaffle)
        }
    }
}

@Composable
private fun RatifyButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(27.dp),
        border = BorderStroke(1.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPesos(value: Double?): String? =
    value?.let { "$ " + "%,.2f".format(Locale.forLanguageTag("es-AR"), it) }