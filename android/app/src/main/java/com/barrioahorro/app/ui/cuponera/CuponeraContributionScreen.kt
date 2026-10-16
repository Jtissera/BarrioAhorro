
package com.barrioahorro.app.ui.cuponera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.cuponera.CuponeraProductDto
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
import com.barrioahorro.app.ui.theme.StepperBorder
import com.barrioahorro.app.ui.theme.StepperCream
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.WarmGray
import java.io.File
private val CardShape = RoundedCornerShape(20.dp)

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** Pantalla "Aporte a la cuponera". La navegación se conecta por afuera con los callbacks. */
@Composable
fun CuponeraContributionScreen(
    onBack: () -> Unit,
    onRatified: () -> Unit = {},
    viewModel: CuponeraViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CuponeraEvent.Ratified) onRatified()
        }
    }

    val actions = CuponeraActions(
        onBack = onBack,
        onRetry = viewModel::load,
        onNombre = viewModel::onNombreChange,
        onValor = viewModel::onValorChange,
        onDelta = viewModel::onUnidadesDelta,
        onRemove = viewModel::onRemove,
        onAdd = viewModel::onAdd,
        onSuggestion = viewModel::onSuggestion,
        onRatify = viewModel::onRatify,
        onPhotoCaptured = viewModel::onPhotoCaptured,
    )

    CuponeraContributionContent(state = state, actions = actions)
}

private class CuponeraActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onNombre: (Long, String) -> Unit,
    val onValor: (Long, String) -> Unit,
    val onDelta: (Long, Int) -> Unit,
    val onRemove: (Long) -> Unit,
    val onAdd: () -> Unit,
    val onSuggestion: (String) -> Unit,
    val onRatify: () -> Unit,
    val onPhotoCaptured: (Long, File) -> Unit,
)

@Composable
private fun CuponeraContributionContent(
    state: CuponeraUiState,
    actions: CuponeraActions,
) {
    val error = state.error

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = Cream,
        topBar = { TopBar(actions.onBack) },
        bottomBar = {
            if (state.commitment != null) BottomBar(state, actions)
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary,
                    )
                }

                state.commitment == null && error != null -> {
                    ErrorState(
                        error,
                        actions.onRetry,
                        Modifier.align(Alignment.Center),
                    )
                }

                state.commitment != null -> ProductList(state, actions)
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .statusBarsPadding(),
    ) {
        // Toldo decorativo: franjas rojas sobre el mismo fondo crema del splash.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            repeat(14) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            if (index % 2 == 0) Primary
                            else Cream,
                        ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Ink,
                )
            }

            Text(
                text = "Aporte A La Cuponera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductList(
    state: CuponeraUiState,
    actions: CuponeraActions,
) {
    val commitment = state.commitment ?: return
    val count = if (state.readOnly) commitment.productos.size else state.validCount

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "¿Qué productos vas a aportar?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                )

                Text(
                    text = "Sumá entre ${state.minProductos} y ${state.maxProductos} productos con stock disponible para este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
        }

        item {
            ProgressCard(
                count = count,
                min = state.minProductos,
                max = state.maxProductos,
                period = formatPeriod(
                    commitment.periodoInicio,
                    commitment.periodoFin,
                ),
            )
        }

        if (state.readOnly) {
            items(commitment.productos, key = { it.id }) { product ->
                ReadOnlyProductCard(product)
            }
        } else {
            items(state.drafts, key = { it.localId }) { draft ->
                DraftProductCard(
                    draft = draft,
                    pesosPorPunto = state.pesosPorPunto,
                    onNombre = { actions.onNombre(draft.localId, it) },
                    onValor = { actions.onValor(draft.localId, it) },
                    onDelta = { actions.onDelta(draft.localId, it) },
                    onRemove = { actions.onRemove(draft.localId) },
                    onPhotoCaptured = { file ->
                        actions.onPhotoCaptured(draft.localId, file)
                    },
                )
            }

            item {
                AddProductButton(
                    enabled = state.canAdd,
                    onClick = actions.onAdd,
                )
            }

            item {
                Suggestions(
                    suggestions = state.suggestions,
                    chipsEnabled = state.canAddOrFill,
                    otherEnabled = state.canAdd,
                    onSuggestion = actions.onSuggestion,
                    onOther = actions.onAdd,
                )
            }

            item { InfoNote() }
        }
    }
}

@Composable
private fun ProgressCard(
    count: Int,
    min: Int,
    max: Int,
    period: String,
) {
    val ready = count >= min

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CardWhite,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count de $max productos",
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )

                if (ready) {
                    Pill(
                        text = "Cupo mínimo listo",
                        container = Secondary,
                        content = OnPrimary,
                    )
                } else {
                    Pill(
                        text = "Faltan ${min - count}",
                        container = PrimaryContainer,
                        content = Primary,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { count.toFloat() / maxOf(1, max) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (ready) {
                    Secondary
                } else {
                    Primary
                },
                trackColor = SurfaceSecondary,
            )

            if (period.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WarmGray,
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = period,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
        )
    }
}

@Composable
private fun PhotoPlaceholder(name: String) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PrimaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "+",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )
    }
}

@Composable
private fun DraftProductCard(
    draft: DraftProduct,
    pesosPorPunto: Int,
    onNombre: (String) -> Unit,
    onValor: (String) -> Unit,
    onDelta: (Int) -> Unit,
    onRemove: () -> Unit,
    onPhotoCaptured: (File) -> Unit,
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CardWhite,
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CuponeraProductPhoto(
                    nombre = draft.nombre,
                    fotoUrl = draft.fotoUrl,
                    fotoSubiendo = draft.fotoSubiendo,
                    editable = true,
                    onPhotoCaptured = onPhotoCaptured,
                )

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = draft.nombre,
                    onValueChange = onNombre,
                    placeholder = {
                        Text(
                            "Nombre del producto",
                            color = WarmGray,
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Ink,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldBackground,
                        unfocusedContainerColor = FieldBackground,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        focusedIndicatorColor = MutedLabel,
                        unfocusedIndicatorColor = MutedLabel,
                    ),
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Quitar producto",
                        tint = Primary,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceSecondary,
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LabeledBlock(
                            title = "VALOR EN TU COMERCIO",
                            subtitle = "Precio de venta actual",
                            modifier = Modifier.weight(1f),
                        )

                        OutlinedTextField(
                            value = draft.valorDigits,
                            onValueChange = onValor,
                            placeholder = {
                                Text(
                                    "0",
                                    color = WarmGray,
                                )
                            },
                            prefix = {
                                Text(
                                    "$ ",
                                    color = Ink,
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = FieldBackground,
                                unfocusedContainerColor = FieldBackground,
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink,
                                focusedIndicatorColor = MutedLabel,
                                unfocusedIndicatorColor = MutedLabel,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            visualTransformation = ThousandsTransformation,
                            modifier = Modifier
                                .width(116.dp)
                                .height(48.dp),
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LabeledBlock(
                            title = "UNIDADES DISPONIBLES",
                            subtitle = "Stock para el ciclo",
                            modifier = Modifier.weight(1f),
                        )

                        Stepper(
                            value = draft.unidades,
                            onDelta = onDelta,
                        )
                    }
                }
            }

            val valor = draft.valor

            if (valor != null && valor > 0) {
                Text(
                    text = "Los vecinos lo canjean por ${pointsFor(valor, pesosPorPunto)} puntos",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyProductCard(product: CuponeraProductDto) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CardWhite,
        ),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CuponeraProductPhoto(
                nombre = product.nombreProducto,
                fotoUrl = product.fotoUrl,
                fotoSubiendo = false,
                editable = false,
                onPhotoCaptured = {},
            )

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    product.nombreProducto,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                )

                product.valorComercio?.let {
                    Text(
                        text = "Valor en tu comercio: $ ${formatThousands(it.toLong().toString())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }

                Text(
                    text = "Canjeados ${product.unidadesCanjeadas} de ${product.unidadesDisponibles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }

            Pill(
                text = "${product.costoPuntos} pts",
                container = PrimaryContainer,
                content = Primary,
            )
        }
    }
}

@Composable
private fun LabeledBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarmGray,
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    onDelta: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FieldBackground,
        border = BorderStroke(1.dp, StepperBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            StepButton("−", "Restar una unidad") {
                onDelta(-1)
            }

            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Ink,
                modifier = Modifier.widthIn(min = 18.dp),
            )

            StepButton("+", "Sumar una unidad") {
                onDelta(1)
            }
        }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(StepperCream, CircleShape)
            .border(
                1.dp,
                StepperBorder,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 18.sp,
            color = Primary,
        )
    }
}

@Composable
private fun AddProductButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CardShape,
        color = CardWhite,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+ Agregar producto",
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    Primary
                } else {
                    MutedLabel
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(
    suggestions: List<String>,
    chipsEnabled: Boolean,
    otherEnabled: Boolean,
    onSuggestion: (String) -> Unit,
    onOther: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SUGERENCIAS RÁPIDAS PARA TU RUBRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarmGray,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { name ->
                SuggestionChip(
                    label = name,
                    enabled = chipsEnabled,
                ) {
                    onSuggestion(name)
                }
            }

            SuggestionChip(
                label = "Otro producto",
                enabled = otherEnabled,
                onClick = onOther,
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = CardWhite,
        border = BorderStroke(
            1.dp,
            if (enabled) {
                MutedLabel
            } else {
                SurfaceSecondary
            },
        ),
    ) {
        Text(
            text = "$label +",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) {
                Ink
            } else {
                WarmGray
            },
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 8.dp,
            ),
        )
    }
}

@Composable
private fun InfoNote() {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(18.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "Una vez ratificado, no podés reducir el stock hasta que termine el período.",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
        )
    }
}

@Composable
private fun BottomBar(
    state: CuponeraUiState,
    actions: CuponeraActions,
) {
    val commitment = state.commitment ?: return

    Surface(
        color = Cream,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                commitment.estado == "RATIFICADO" -> {
                    StatusBanner(
                        text = "Aporte ratificado",
                        container = Secondary,
                        content = OnPrimary,
                    )
                }

                state.readOnly -> {
                    StatusBanner(
                        text = "El plazo para ratificar este período venció",
                        container = SurfaceSecondary,
                        content = ErrorRed,
                    )
                }

                else -> {
                    state.submitError?.let {
                        Text(
                            text = it,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Button(
                        onClick = actions.onRatify,
                        enabled = state.canSubmit,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = OnPrimary,
                            disabledContainerColor = MutedLabel,
                            disabledContentColor = OnPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = OnPrimary,
                            )
                        } else {
                            Text(
                                "Ratificar aporte",
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Text(
                        text = "Mínimo ${state.minProductos} productos para continuar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            color = content,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = Ink,
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = OnPrimary,
            ),
        ) {
            Text("Reintentar")
        }
    }
}

private fun formatThousands(digits: String): String =
    digits.reversed().chunked(3).joinToString(".").reversed()

/** Muestra 6000 como 6.000 sin tocar el valor guardado (solo dígitos). */
private object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatThousands(text.text)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var digits = 0
                var t = 0

                while (digits < offset && t < formatted.length) {
                    if (formatted[t].isDigit()) digits++
                    t++
                }

                return t
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it.isDigit() }
        }

        return TransformedText(
            AnnotatedString(formatted),
            mapping,
        )
    }
}

private fun parseDate(iso: String): Triple<Int, Int, Int>? {
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null

    return Triple(year, month, day)
}

/** "Del 12 al 26 de octubre" (sin java.time, para no depender del minSdk). */
private fun formatPeriod(
    inicio: String,
    fin: String,
): String {
    val a = parseDate(inicio) ?: return ""
    val b = parseDate(fin) ?: return ""

    return if (a.second == b.second) {
        "Del ${a.third} al ${b.third} de ${MONTHS[b.second - 1]}"
    } else {
        "Del ${a.third} de ${MONTHS[a.second - 1]} al ${b.third} de ${MONTHS[b.second - 1]}"
    }
}
