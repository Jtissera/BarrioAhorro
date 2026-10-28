package com.barrioahorro.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.data.admin.AdminPublicationDto
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.cuponera.ContributionErrorState
import com.barrioahorro.app.ui.cuponera.ContributionTopBar
import com.barrioahorro.app.ui.cuponera.StatusPill
import com.barrioahorro.app.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront

private val esAR = Locale.forLanguageTag("es-AR")

// ---------- Lista ----------

@Composable
fun AdminListScreen(
    onOpen: (kind: String, id: Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding()) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "Moderación de Publicaciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = WarmGray)
                }
            }
            StatusPill("ROL MODERADOR", SecondaryContainer, Secondary)
            Text(
                "Revisá los cupones y sorteos enviados por los comerciantes del barrio.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Todas", state.items.size, state.filter == AdminFilter.ALL) { viewModel.setFilter(AdminFilter.ALL) }
                FilterPill("Cupones", state.cuponCount, state.filter == AdminFilter.CUPON) { viewModel.setFilter(AdminFilter.CUPON) }
                FilterPill("Sorteos", state.sorteoCount, state.filter == AdminFilter.SORTEO) { viewModel.setFilter(AdminFilter.SORTEO) }
            }
        }

        when {
            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            state.error != null && state.items.isEmpty() ->
                ContributionErrorState(state.error.orEmpty(), viewModel::load, Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("Pendientes de revisión", BadgeYellow, BadgeYellowText)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Mostrando ${state.visible.size} solicitudes",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray,
                        )
                    }
                }
                if (state.visible.isEmpty()) {
                    item {
                        Text(
                            "No hay publicaciones pendientes de revisión.",
                            color = WarmGray,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.visible, key = { "${it.kind}-${it.id}" }) { item ->
                    PublicationCard(item) { onOpen(item.kind, item.id) }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else SurfaceMuted,
        contentColor = if (selected) OnPrimary else Ink,
    ) {
        Text(
            "$title  $count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PublicationCard(item: AdminPublicationDto, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.businessName ?: "Comercio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Text(formatSubmitted(item.submittedAt), style = MaterialTheme.typography.labelSmall, color = WarmGray)
                }
                KindTag(item.kind)
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = WarmGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusPill("Pendiente de revisión", BadgeYellow, BadgeYellowText)
                Spacer(Modifier.weight(1f))
                Text("Revisar publicación →", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    }
}

// ---------- Detalle ----------

@Composable
fun AdminDetailScreen(
    onBack: () -> Unit,
    onReject: (AdminPublicationDto) -> Unit,
    onDone: () -> Unit,
    viewModel: AdminReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }
    val item = state.item

    Scaffold(
        containerColor = Cream,
        topBar = { ContributionTopBar(title = if (item?.kind == "SORTEO") "Revisión del sorteo" else "Revisión del cupón", onBack = onBack) },
        bottomBar = {
            if (item != null) {
                Surface(color = Cream, shadowElevation = 8.dp) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        state.actionError?.let {
                            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onReject(item) },
                                enabled = !state.submitting,
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary),
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rechazar", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = viewModel::approve,
                                enabled = !state.submitting,
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary, contentColor = OnPrimary),
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) {
                                if (state.submitting) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Aprobar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            item != null -> DetailContent(item, Modifier.padding(padding))
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            else -> ContributionErrorState(state.error ?: "No se pudo cargar la publicación.", viewModel::load, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(item: AdminPublicationDto, modifier: Modifier = Modifier) {
    val isRaffle = item.kind == "SORTEO"
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Franja oscura: estado y hora
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill("Pendiente de aprobación", BadgeYellow, BadgeYellowText)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = WarmGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatSubmitted(item.submittedAt), style = MaterialTheme.typography.labelSmall, color = Ink)
            }
        }

        // Comercio
        Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(SecondaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Secondary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        item.businessName ?: "Comercio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    item.businessAddress?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = WarmGray)
                    }
                }
            }
        }

        // Tarjeta blanca principal
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                PhotoHeader(item)

                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "BENEFICIO PROPUESTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )

                    item.description?.takeIf { it.isNotBlank() }?.let {
                        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceFieldAlt , modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = WarmGray, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Descripción enviada",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmGray,
                                    )
                                }
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isRaffle) {
                            StatCard(
                                icon = Icons.Filled.Star,
                                label = "Valor del premio",
                                value = item.amount?.let(::money) ?: "—",
                                unit = "",
                                accent = Primary,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            StatCard(
                                icon = Icons.Filled.Star,
                                label = "Costo para el vecino",
                                value = "${item.points ?: "—"}",
                                unit = "puntos",
                                accent = Primary,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                icon = Icons.Outlined.Inventory2,
                                label = "Stock por ciclo",
                                value = "${item.stock ?: "—"}",
                                unit = "unidades",
                                accent = Secondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceFieldAlt, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Checklist, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Criterios de moderación barrial",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                )
                            }
                            if (isRaffle) {
                                Criterion("El premio es claro y atractivo para los vecinos")
                                Criterion("El valor declarado es coherente con el premio")
                            } else {
                                Criterion("El valor en puntos es coherente con el ticket promedio")
                                Criterion("La foto representa fielmente el producto ofrecido")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Foto del comercio con sus etiquetas, o un aviso claro si no cargó ninguna. */
@Composable
private fun PhotoHeader(item: AdminPublicationDto) {
    val isRaffle = item.kind == "SORTEO"
    val photo = item.photoUrl?.takeIf { it.isNotBlank() }

    Box(Modifier.fillMaxWidth().height(200.dp)) {
        if (photo != null) {
            AsyncImage(
                model = photo.toAbsoluteUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(SurfaceFieldAlt),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    tint = MutedLabel,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                     "El comercio no cargó una foto",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmGray,
                )
            }
        }

        // Etiqueta superior izquierda
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = BadgeYellow,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isRaffle) Icons.Filled.CardGiftcard else Icons.Outlined.Description,
                    contentDescription = null,
                    tint = BadgeYellowText,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isRaffle) "Sorteo Barrial" else "Cupón Barrial",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BadgeYellowText,
                )
            }
        }

        // Etiqueta inferior derecha
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardWhite.copy(alpha = 0.92f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (photo != null) Icons.Outlined.PhotoCamera else Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (photo != null) "Foto del comercio" else "Sin foto",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceFieldAlt, modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Ink)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = accent)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodySmall, color = WarmGray, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun Criterion(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).background(Secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink)
    }
}

// ---------- Rechazo ----------

@Composable
fun AdminRejectScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AdminReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }
    val item = state.item
    val length = state.reason.trim().length

    Scaffold(
        containerColor = Cream,
        modifier = Modifier.imePadding(),
        topBar = { ContributionTopBar(title = "Detalle de la publicación", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("RESOLUCIÓN MODERADORA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Primary)
            Text("Rechazar publicación", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(
                "Explicá al comerciante el motivo del rechazo para que pueda corregirlo y volver a postularlo.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )

            if (item != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RESUMEN", style = MaterialTheme.typography.labelSmall, color = WarmGray, modifier = Modifier.weight(1f))
                            KindTag(item.kind)
                        }
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                        Text(item.businessName ?: "Comercio", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                    }
                }
            }

            Text("Motivo del rechazo *", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Ink)
            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::onReasonChange,
                placeholder = { Text("Escribí detalladamente el motivo para el comerciante… (obligatorio)") },
                minLines = 6,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = StepperBorder,
                    cursorColor = Primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                Text(
                    "Mínimo $REJECT_MIN caracteres requeridos",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (length in 1 until REJECT_MIN) ErrorRed else WarmGray,
                    modifier = Modifier.weight(1f),
                )
                Text("$length/$REJECT_MAX", style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }

            state.actionError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = viewModel::reject,
                enabled = state.canReject,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = OnPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Confirmar rechazo", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("← Cancelar y volver", color = Ink, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Helpers ----------

@Composable
private fun KindTag(kind: String) {
    if (kind == "SORTEO") StatusPill("Sorteo", PrimaryContainer, Primary)
    else StatusPill("Cupón", BadgeYellow, BadgeYellowText)
}

private fun formatSubmitted(iso: String?): String {
    val dt = iso?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() }
        ?: return ""
    val today = LocalDate.now()
    val date = dt.toLocalDate()
    val day = when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMM", esAR))
    }
    return "$day, ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))} hs"
}

private fun money(value: Double): String {
    val nf = NumberFormat.getNumberInstance(esAR).apply {
        maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = maximumFractionDigits
    }
    return "$" + nf.format(value)
}

private fun String.toAbsoluteUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this else "${BASE_URL.trimEnd('/')}/${trimStart('/')}"