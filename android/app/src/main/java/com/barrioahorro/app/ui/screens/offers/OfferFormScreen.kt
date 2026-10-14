package com.barrioahorro.app.ui.screens.offers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esAR: Locale = Locale.forLanguageTag("es-AR")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val presetDiscounts = listOf(15, 20, 25, 30)

/** Colores de colors.xml que usa el formulario. */
private data class FormColors(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val secondary: Color,
    val card: Color,
    val field: Color,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val inactive: Color,
    val border: Color,
    val error: Color,
)

@Composable
private fun formColors() = FormColors(
    primary = colorResource(R.color.primary),
    primaryContainer = colorResource(R.color.primary_container),
    onPrimary = colorResource(R.color.on_primary),
    secondary = colorResource(R.color.secondary),
    card = colorResource(R.color.surface_card),
    field = colorResource(R.color.stepper_cream),
    background = colorResource(R.color.splash_background),
    textPrimary = colorResource(R.color.text_primary),
    textSecondary = colorResource(R.color.text_secondary),
    inactive = colorResource(R.color.neutral_inactive),
    border = colorResource(R.color.stepper_border),
    error = colorResource(R.color.error),
)

@Composable
fun OfferFormScreen(
    state: CreateOfferUiState,
    onProductNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (Int?) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onValidUntilChange: (LocalDateTime) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = formColors()
    val isLastUnit = state.selectedType == OfferType.POR_CANTIDAD
    val price = state.price.toPriceOrNull()
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            BottomActions(
                isSubmitting = state.isSubmitting,
                canSubmit = state.canSubmit,
                onBack = onBack,
                onSubmit = onSubmit,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (isLastUnit) "Crear oferta por cantidad" else "Crear oferta porcentual",
                style = MaterialTheme.typography.headlineMedium,
                color = c.textPrimary,
            )
            Pill(
                text = if (isLastUnit) "DESCUENTO POR CANTIDAD" else "DESCUENTO PORCENTUAL",
                icon = Icons.Filled.LocalOffer,
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = c.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel("Producto", required = true)
                        FormTextField(
                            value = state.productName,
                            onValueChange = onProductNameChange,
                            placeholder = "Ej: Tomate perita x 2 kg",
                        )
                    }

                    PhotoPlaceholder()

                    if (isLastUnit) {
                        QuantityConditions(
                            state = state,
                            price = price,
                            onPriceChange = onPriceChange,
                            onDiscountChange = onDiscountChange,
                            onIncrement = onIncrementQuantity,
                            onDecrement = onDecrementQuantity,
                        )
                    } else {
                        PercentageConditions(
                            state = state,
                            onPriceChange = onPriceChange,
                            onDiscountChange = onDiscountChange,
                        )
                    }

                    ValiditySection(
                        validUntil = state.validUntil,
                        onPickClick = { showDatePicker = true },
                    )
                }
            }

            OfferPreview(state = state, price = price, isLastUnit = isLastUnit)

            state.error?.let {
                Text(it, color = c.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = c.secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Se publica enseguida, no necesita aprobación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }

    // Paso 1 del picker: la fecha
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isBefore(LocalDate.now())
                }
            },
        )
        val pickerColors = DatePickerDefaults.colors(
            containerColor = c.card,
            selectedDayContainerColor = c.primary,
            selectedDayContentColor = c.onPrimary,
            todayDateBorderColor = c.primary,
            todayContentColor = c.primary,
            selectedYearContainerColor = c.primary,
            selectedYearContentColor = c.onPrimary,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.primary),
                ) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.primary),
                ) { Text("Cancelar") }
            },
            colors = pickerColors,
        ) {
            DatePicker(state = datePickerState, colors = pickerColors)
        }
    }

    // Paso 2 del picker: la hora
    pickedDate?.let { date ->
        val timePickerState = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickedDate = null },
            containerColor = c.card,
            title = { Text("¿Hasta qué hora?", color = c.textPrimary) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = c.field,
                        selectorColor = c.primary,
                        clockDialSelectedContentColor = c.onPrimary,
                        timeSelectorSelectedContainerColor = c.primaryContainer,
                        timeSelectorSelectedContentColor = c.primary,
                        timeSelectorUnselectedContainerColor = c.field,
                        timeSelectorUnselectedContentColor = c.textPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValidUntilChange(date.atTime(timePickerState.hour, timePickerState.minute))
                        pickedDate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.primary),
                ) { Text("Listo") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pickedDate = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.primary),
                ) { Text("Cancelar") }
            },
        )
    }
}

// ---------- Secciones ----------

@Composable
private fun PercentageConditions(
    state: CreateOfferUiState,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (Int?) -> Unit,
) {
    val c = formColors()
    var customMode by rememberSaveable {
        mutableStateOf(state.discountPercent.let { it != null && it !in presetDiscounts })
    }

    Surface(shape = RoundedCornerShape(16.dp), color = c.primaryContainer.copy(alpha = 0.5f)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Porcentaje de rebaja", required = true, color = c.primary, modifier = Modifier.weight(1f))
                state.discountPercent?.let {
                    Text("-$it%", style = MaterialTheme.typography.titleLarge, color = c.primary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presetDiscounts.forEach { p ->
                    val selected = !customMode && state.discountPercent == p
                    Surface(
                        onClick = { customMode = false; onDiscountChange(p) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) c.primary else c.card,
                        contentColor = if (selected) c.onPrimary else c.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-$p%", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (customMode) {
                FormTextField(
                    value = state.discountPercent?.toString() ?: "",
                    onValueChange = { onDiscountChange(it.filter(Char::isDigit).take(2).toIntOrNull()) },
                    placeholder = "Ej: 35",
                    suffix = "%",
                    keyboardType = KeyboardType.Number,
                    containerColor = c.card,
                )
            } else {
                Text(
                    "Otro porcentaje",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { customMode = true; onDiscountChange(null) },
                )
            }

            Text(
                "El vecino verá el descuento directo sobre el precio regular del mostrador.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Precio original ($)")
                    FormTextField(
                        value = state.price,
                        onValueChange = { onPriceChange(it.sanitizePrice()) },
                        prefix = "$ ",
                        keyboardType = KeyboardType.Decimal,
                        containerColor = c.card,
                        textStyle = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Precio final cliente", color = c.secondary)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = c.secondary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Paga", style = MaterialTheme.typography.labelMedium, color = c.secondary, modifier = Modifier.weight(1f))
                            Text(
                                state.previewDiscountedPrice?.let(::formatMoney) ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityConditions(
    state: CreateOfferUiState,
    price: BigDecimal?,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (Int?) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val c = formColors()
    val n = state.requiredQuantity
    val discount = state.discountPercent
    val discounted = state.previewDiscountedPrice

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(
                "Condiciones de cantidad",
                required = true,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (discount != null) Pill("${ordinal(n)} al $discount%")
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Precio regular por unidad", required = true)
            FormTextField(
                value = state.price,
                onValueChange = { onPriceChange(it.sanitizePrice()) },
                prefix = "$ ",
                suffix = "c/u",
                keyboardType = KeyboardType.Decimal,
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FieldLabel("Cantidad req.", required = true, color = c.primary, modifier = Modifier.fillMaxWidth())
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.field,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = onDecrement, enabled = n > 2) {
                            Icon(Icons.Filled.Remove, contentDescription = "Menos", tint = c.textPrimary)
                        }
                        Text("$n", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        IconButton(onClick =