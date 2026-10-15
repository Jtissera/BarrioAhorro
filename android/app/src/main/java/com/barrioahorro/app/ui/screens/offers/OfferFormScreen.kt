package com.barrioahorro.app.ui.screens.offers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.barrioahorro.app.R
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.components.ToldoConFranjas
import java.io.File
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferFormScreen(
    state: CreateOfferUiState,
    onProductNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (Int?) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onUnitsToTakeIncrement: () -> Unit = {},
    onUnitsToTakeDecrement: () -> Unit = {},
    onUnitsToPayIncrement: () -> Unit = {},
    onUnitsToPayDecrement: () -> Unit = {},
    onWholesalePriceChange: (String) -> Unit = {},
    onMinQuantityChange: (Int) -> Unit = {},
    onMinQuantityIncrement: () -> Unit = {},
    onMinQuantityDecrement: () -> Unit = {},
    onVigenciaTipoChange: (VigenciaTipo) -> Unit = {},
    onValidUntilChange: (LocalDateTime?) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = formColors()
    val isTwoForOne = state.selectedType == OfferType.DOS_POR_UNO
    val isWholesale = state.selectedType == OfferType.POR_MAYOR
    val isLastUnit = state.selectedType == OfferType.POR_CANTIDAD
    val price = state.price.toPriceOrNull()
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showVigenciaSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.background,
        topBar = {
            ToldoConFranjas(
                modifier = Modifier
                    .background(colorResource(R.color.splash_background))
                    .statusBarsPadding(),
            )
        },
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
                when {
                    isTwoForOne -> "Crear oferta 2×1"
                    isWholesale -> "Crear oferta por mayor"
                    isLastUnit -> "Crear oferta por cantidad"
                    else -> "Crear oferta porcentual"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Pill(
                text = when {
                    isTwoForOne -> "PROMOCIÓN 2X1"
                    isWholesale -> "VENTA POR MAYOR"
                    isLastUnit -> "DESCUENTO POR CANTIDAD"
                    else -> "DESCUENTO PORCENTUAL"
                },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FieldLabel("Producto", required = true)
                            Text(
                                "Reconocido por vecinos",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textSecondary,
                            )
                        }
                        FormTextField(
                            value = state.productName,
                            onValueChange = onProductNameChange,
                            placeholder = when {
                                isTwoForOne -> "Ej: Limón tucumano seleccionado"
                                isWholesale -> "Ej: Leche entera sachet 1L"
                                else -> "Ej: Tomate perita x 2 kg"
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.ShoppingBag,
                                    contentDescription = null,
                                    tint = c.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }

                    PhotoSection(
                        photoUrl = state.photoUrl,
                        isUploading = state.isUploadingPhoto,
                        onPhotoPicked = onPhotoPicked,
                        onRemove = onRemovePhoto,
                    )

                    when {
                        isTwoForOne -> {
                            TwoForOneConditions(
                                state = state,
                                price = price,
                                onPriceChange = onPriceChange,
                                onUnitsToTakeIncrement = onUnitsToTakeIncrement,
                                onUnitsToTakeDecrement = onUnitsToTakeDecrement,
                                onUnitsToPayIncrement = onUnitsToPayIncrement,
                                onUnitsToPayDecrement = onUnitsToPayDecrement,
                            )
                        }
                        isWholesale -> {
                            WholesaleConditions(
                                state = state,
                                onPriceChange = onPriceChange,
                                onWholesalePriceChange = onWholesalePriceChange,
                                onMinQuantityChange = onMinQuantityChange,
                                onMinQuantityIncrement = onMinQuantityIncrement,
                                onMinQuantityDecrement = onMinQuantityDecrement,
                            )
                        }
                        isLastUnit -> {
                            QuantityConditions(
                                state = state,
                                price = price,
                                onPriceChange = onPriceChange,
                                onDiscountChange = onDiscountChange,
                                onIncrement = onIncrementQuantity,
                                onDecrement = onDecrementQuantity,
                            )
                        }
                        else -> {
                            PercentageConditions(
                                state = state,
                                onPriceChange = onPriceChange,
                                onDiscountChange = onDiscountChange,
                            )
                        }
                    }

                    ValiditySection(
                        vigenciaTipo = state.vigenciaTipo,
                        validUntil = state.validUntil,
                        onPickVigenciaClick = { showVigenciaSheet = true },
                    )
                }
            }

            OfferPreview(
                state = state,
                price = price,
                isLastUnit = isLastUnit,
                isTwoForOne = isTwoForOne,
                isWholesale = isWholesale,
            )

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

    if (showVigenciaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVigenciaSheet = false },
            containerColor = c.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Vigencia y disponibilidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                )
                Text(
                    "Elegí cuánto tiempo estará disponible esta oferta para los vecinos:",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )

                VigenciaOptionItem(
                    title = "Hasta agotar stock",
                    subtitle = "Ideal para liquidar stock rápido. Permanece activa hasta agotar.",
                    icon = Icons.Outlined.Schedule,
                    isSelected = state.vigenciaTipo == VigenciaTipo.HASTA_AGOTAR_STOCK,
                    onClick = {
                        onVigenciaTipoChange(VigenciaTipo.HASTA_AGOTAR_STOCK)
                        onValidUntilChange(null)
                        showVigenciaSheet = false
                    },
                )

                VigenciaOptionItem(
                    title = "Solo hoy",
                    subtitle = "Válida hasta las 23:59 de hoy.",
                    icon = Icons.Outlined.Today,
                    isSelected = state.vigenciaTipo == VigenciaTipo.SOLO_HOY,
                    onClick = {
                        onVigenciaTipoChange(VigenciaTipo.SOLO_HOY)
                        onValidUntilChange(null)
                        showVigenciaSheet = false
                    },
                )

                VigenciaOptionItem(
                    title = "Toda la semana",
                    subtitle = "Válida durante 7 días corridos a partir de hoy.",
                    icon = Icons.Outlined.DateRange,
                    isSelected = state.vigenciaTipo == VigenciaTipo.TODA_LA_SEMANA,
                    onClick = {
                        onVigenciaTipoChange(VigenciaTipo.TODA_LA_SEMANA)
                        onValidUntilChange(null)
                        showVigenciaSheet = false
                    },
                )

                VigenciaOptionItem(
                    title = "Fecha específica",
                    subtitle = "Elegí el día y horario límite exacto en el calendario.",
                    icon = Icons.Outlined.CalendarMonth,
                    isSelected = state.vigenciaTipo == VigenciaTipo.FECHA_ESPECIFICA,
                    onClick = {
                        onVigenciaTipoChange(VigenciaTipo.FECHA_ESPECIFICA)
                        showVigenciaSheet = false
                        showDatePicker = true
                    },
                )
            }
        }
    }
}

// ---------- Secciones ----------

@Composable
private fun TwoForOneConditions(
    state: CreateOfferUiState,
    price: BigDecimal?,
    onPriceChange: (String) -> Unit,
    onUnitsToTakeIncrement: () -> Unit,
    onUnitsToTakeDecrement: () -> Unit,
    onUnitsToPayIncrement: () -> Unit,
    onUnitsToPayDecrement: () -> Unit,
) {
    val c = formColors()
    val llevas = state.unitsToTake
    val pagas = state.unitsToPay
    val savingsPercent = state.preview2x1SavingsPercent
    val totalPromo = state.preview2x1TotalPromo
    val unitPrice = state.preview2x1UnitPrice

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel("Configuración de la oferta", required = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFE0D2),
            ) {
                Text(
                    "Promoción por volumen",
                    color = c.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Llevás
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Llevás", required = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.field,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = c.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onUnitsToTakeDecrement, enabled = llevas > 2, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Remove, contentDescription = "Menos", tint = if (llevas > 2) c.textPrimary else c.inactive)
                            }
                            Text(
                                "$llevas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            IconButton(onClick = onUnitsToTakeIncrement, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Más", tint = c.textPrimary)
                            }
                        }
                    }
                }
            }

            // Pagás
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Pagás", required = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.field,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(
                            Icons.Outlined.ConfirmationNumber,
                            contentDescription = null,
                            tint = c.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onUnitsToPayDecrement, enabled = pagas > 1, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Remove, contentDescription = "Menos", tint = if (pagas > 1) c.textPrimary else c.inactive)
                            }
                            Text(
                                "$pagas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            IconButton(onClick = onUnitsToPayIncrement, enabled = pagas < llevas - 1, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Más", tint = if (pagas < llevas - 1) c.textPrimary else c.inactive)
                            }
                        }
                    }
                }
            }
        }

        // Tarjeta dinámica de ahorro
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF2EB),
            border = BorderStroke(1.dp, Color(0xFFFFD9C7)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(c.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFE0D2),
                    ) {
                        Text(
                            "${llevas}X${pagas} ACTIVO",
                            color = c.primary,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$savingsPercent% de ahorro directo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C6B4F),
                    )
                }
                Text(
                    "El vecino paga $pagas unidad${if (pagas > 1) "es" else ""} y se lleva $llevas. Promoción clásica que liquida stock rápido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }

        // Precio unitario regular
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Precio unitario regular", required = true)
            FormTextField(
                value = state.price,
                onValueChange = { onPriceChange(it.sanitizePrice()) },
                placeholder = "950",
                prefix = "$ ",
                keyboardType = KeyboardType.Decimal,
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }

        // Resumen total promo
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE8F7F0),
            border = BorderStroke(1.dp, Color(0xFFBCE8D3)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = Color(0xFF1E824C),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Total promo para el cliente:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1B5E39),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (totalPromo != null && unitPrice != null) {
                        Text(
                            formatMoney(totalPromo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E39),
                        )
                        Text(
                            "(${formatMoney(unitPrice)} c/u)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                        )
                    } else {
                        Text(
                            "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF888888),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WholesaleConditions(
    state: CreateOfferUiState,
    onPriceChange: (String) -> Unit,
    onWholesalePriceChange: (String) -> Unit,
    onMinQuantityChange: (Int) -> Unit,
    onMinQuantityIncrement: () -> Unit,
    onMinQuantityDecrement: () -> Unit,
) {
    val c = formColors()
    val savings = state.previewWholesaleSavingsPercent
    val regularPrice = state.priceValue
    val wholesalePrice = state.wholesalePriceValue

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel("Condiciones por mayor", required = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFE0D2),
            ) {
                Text(
                    "Desde ${state.minQuantity} u.",
                    color = c.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // Precio regular unitario
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Precio regular unitario", required = true)
            FormTextField(
                value = state.price,
                onValueChange = { onPriceChange(it.sanitizePrice()) },
                placeholder = "1050",
                prefix = "$ ",
                keyboardType = KeyboardType.Decimal,
                containerColor = c.field,
            )
        }

        // Precio por mayor
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Precio por mayor", required = true)
            FormTextField(
                value = state.wholesalePrice,
                onValueChange = { onWholesalePriceChange(it.sanitizePrice()) },
                placeholder = "850",
                prefix = "$ ",
                keyboardType = KeyboardType.Decimal,
                containerColor = c.field,
            )
        }

        // Cantidad mínima
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Cantidad mínima", required = true)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.field,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${state.minQuantity} u.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMinQuantityDecrement,
                            enabled = state.minQuantity > 2,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Menos",
                                tint = if (state.minQuantity > 2) c.textPrimary else c.inactive,
                            )
                        }
                        IconButton(onClick = onMinQuantityIncrement, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Más", tint = c.textPrimary)
                        }
                    }
                }
            }
        }

        // Highlight card: Ahorro por bulto cerrado
        if (regularPrice != null && wholesalePrice != null && regularPrice > BigDecimal.ZERO && wholesalePrice < regularPrice && savings != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF0EC),
                border = BorderStroke(1.dp, Color(0xFFFFD6CC)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.primary)) {
                                append("Ahorro por bulto cerrado: ")
                            }
                            append("Precio unitario regular ${formatMoney(regularPrice)} → Precio mayorista ${formatMoney(wholesalePrice)} (-$savings% OFF).")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textPrimary,
                    )
                }
            }
        }
    }
}

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
                        color = colorResource(R.color.secondary_container),
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
                        IconButton(onClick = onIncrement) {
                            Icon(Icons.Filled.Add, contentDescription = "Más", tint = c.textPrimary)
                        }
                    }
                }
                Text("unidades", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FieldLabel("Descuento última", required = true, color = c.primary, modifier = Modifier.fillMaxWidth())
                FormTextField(
                    value = discount?.toString() ?: "",
                    onValueChange = { onDiscountChange(it.filter(Char::isDigit).take(2).toIntOrNull()) },
                    placeholder = "50",
                    suffix = "%",
                    suffixColor = c.primary,
                    keyboardType = KeyboardType.Number,
                    textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.End),
                )
                Text("en ${ordinal(n)} unidad", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            }
        }

        if (price != null && discount != null && discounted != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = c.border.copy(alpha = 0.3f)) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Aplica $discount% de descuento en la ${ordinal(n)} unidad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textPrimary,
                        )
                        Text(
                            buildAnnotatedString {
                                append("Precio regular: ")
                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                    append(formatMoney(price))
                                }
                                append(" → ${ordinal(n)} unidad: ${formatMoney(discounted)}")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = c.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSection(
    photoUrl: String?,
    isUploading: Boolean,
    onPhotoPicked: (Uri) -> Unit,
    onRemove: () -> Unit,
) {
    val c = formColors()
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri?.let(Uri::parse)
        pendingCameraUri = null
        if (ok && uri != null) onPhotoPicked(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }

    fun openCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("oferta_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri.toString()
        cameraLauncher.launch(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel("Foto del producto", required = true)
            if (photoUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "1/1 cargada",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )
                }
            }
        }

        if (photoUrl != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.card),
                ) {
                    AsyncImage(
                        model = photoUrl.toAbsoluteUrl(),
                        contentDescription = "Foto del producto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Text(
                            "1/1",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.field,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isUploading) { showSheet = true },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Cambiar foto",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.primary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Muestra el producto real en góndola",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = shape,
                color = c.field,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable(enabled = !isUploading) { showSheet = true },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.card),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = c.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = c.textSecondary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isUploading) "Subiendo foto…" else "Agregar foto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        Text(
                            "Muestra el producto real en góndola",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = c.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Foto del producto", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                PhotoSourceOption(Icons.Filled.PhotoCamera, "Sacar foto") {
                    showSheet = false
                    openCamera()
                }
                PhotoSourceOption(Icons.Filled.PhotoLibrary, "Elegir de la galería") {
                    showSheet = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        }
    }
}

@Composable
private fun PhotoSourceOption(icon: ImageVector, text: String, onClick: () -> Unit) {
    val c = formColors()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = c.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = c.primary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
        }
    }
}

private fun String.toAbsoluteUrl(): String =
    if (startsWith("http")) this else BASE_URL.trimEnd('/') + "/" + trimStart('/')

@Composable
private fun ValiditySection(
    vigenciaTipo: VigenciaTipo,
    validUntil: LocalDateTime?,
    onPickVigenciaClick: () -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    val c = formColors()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel("Vigencia y disponibilidad", required = true)
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ValidityBox(
                label = "Desde",
                value = "• Hoy, ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                icon = Icons.Outlined.Schedule,
                onClick = null,
                modifier = Modifier.weight(1f),
            )

            val (icon, text) = when (vigenciaTipo) {
                VigenciaTipo.HASTA_AGOTAR_STOCK -> Icons.Outlined.Schedule to "Hasta agotar stock"
                VigenciaTipo.SOLO_HOY -> Icons.Outlined.Today to "Solo hoy (23:59)"
                VigenciaTipo.TODA_LA_SEMANA -> Icons.Outlined.DateRange to "Toda la semana"
                VigenciaTipo.FECHA_ESPECIFICA -> Icons.Outlined.CalendarMonth to (validUntil?.let(::formatRelative) ?: "Elegir fecha")
            }

            ValidityBox(
                label = "Hasta",
                value = text,
                icon = icon,
                onClick = onPickVigenciaClick,
                highlight = vigenciaTipo == VigenciaTipo.FECHA_ESPECIFICA && validUntil == null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VigenciaOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = formColors()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) c.primaryContainer.copy(alpha = 0.4f) else c.field,
        border = if (isSelected) BorderStroke(1.5.dp, c.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isSelected) c.primary else c.card, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) c.onPrimary else c.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ValidityBox(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val c = formColors()
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = c.field,
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) c.primary else c.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun OfferPreview(
    state: CreateOfferUiState,
    price: BigDecimal?,
    isLastUnit: Boolean,
    isTwoForOne: Boolean = false,
    isWholesale: Boolean = false,
) {
    val c = formColors()
    val discount = state.discountPercent
    val discounted = state.previewDiscountedPrice

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = c.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Así la van a ver tus vecinos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFE0D2),
            ) {
                Text(
                    "En vivo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = c.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text("Vista previa en tiempo real en el feed del barrio", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = c.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ToldoConFranjas(stripes = 20, height = 10.dp)
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Surface(shape = RoundedCornerShape(12.dp), color = c.field, modifier = Modifier.size(88.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.photoUrl != null) {
                                AsyncImage(
                                    model = state.photoUrl.toAbsoluteUrl(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize(),
                                )
                            } else {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = c.inactive)
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            if (isTwoForOne) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFE0D2),
                                ) {
                                    Text(
                                        "${state.unitsToTake}X${state.unitsToPay}",
                                        color = c.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else if (isWholesale) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFE0D2),
                                ) {
                                    Text(
                                        "X MAYOR",
                                        color = c.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else if (discount != null) {
                                Pill(if (isLastUnit) "${ordinal(state.requiredQuantity)} al $discount%".uppercase() else "-$discount%")
                            }
                            Spacer(Modifier.weight(1f))
                            PreviewPrice(
                                state = state,
                                price = price,
                                discounted = discounted,
                                isLastUnit = isLastUnit,
                                isTwoForOne = isTwoForOne,
                                isWholesale = isWholesale,
                            )
                        }

                        Text(
                            state.productName.ifBlank {
                                when {
                                    isTwoForOne -> "Limón tucumano seleccionado"
                                    isWholesale -> "Leche entera sachet 1L"
                                    else -> "Tomate perita x 2 kg"
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                state.businessName.ifBlank { "Mi Comercio" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                            )
                            if (state.categoryName.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = c.field) {
                                    Text(
                                        state.categoryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.textSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍 a 250 m", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                            val vigenciaText = when (state.vigenciaTipo) {
                                VigenciaTipo.HASTA_AGOTAR_STOCK -> "Hasta agotar stock"
                                VigenciaTipo.SOLO_HOY -> "Hoy 23:59"
                                VigenciaTipo.TODA_LA_SEMANA -> "Toda la semana"
                                VigenciaTipo.FECHA_ESPECIFICA -> state.validUntil?.let(::formatRelative) ?: "Fecha a definir"
                            }
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(vigenciaText, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F7F0),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color(0xFF1E824C), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isWholesale) "0 vecinos confirmaron disponibilidad" else "0 vecinos confirmaron",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B5E39),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPrice(
    state: CreateOfferUiState,
    price: BigDecimal?,
    discounted: BigDecimal?,
    isLastUnit: Boolean,
    isTwoForOne: Boolean = false,
    isWholesale: Boolean = false,
) {
    val c = formColors()
    Column(horizontalAlignment = Alignment.End) {
        when {
            isTwoForOne -> {
                val unit = state.preview2x1UnitPrice
                if (unit != null) {
                    Text(
                        "${formatMoney(unit)} c/u",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
                } else {
                    Text("$ —", style = MaterialTheme.typography.titleLarge, color = c.textSecondary)
                }
            }
            isWholesale -> {
                val wh = state.wholesalePriceValue
                val reg = state.priceValue
                if (wh != null) {
                    Text(
                        "${formatMoney(wh)} c/u",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                    )
                    if (reg != null && reg > wh) {
                        Text(
                            buildAnnotatedString {
                                append("antes ")
                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                    append(formatMoney(reg))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                } else {
                    Text("$ —", style = MaterialTheme.typography.titleLarge, color = c.textSecondary)
                }
            }
            price == null || discounted == null -> {
                Text("$ —", style = MaterialTheme.typography.titleLarge, color = c.textSecondary)
            }
            isLastUnit -> {
                val n = state.requiredQuantity
                val average = price.multiply(BigDecimal(n - 1)).add(discounted)
                    .divide(BigDecimal(n), 0, RoundingMode.HALF_UP)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatMoney(price),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(formatMoney(average), style = MaterialTheme.typography.titleLarge, color = c.primary)
                }
                Text(
                    "prom. c/u (${ordinal(n)} ${formatMoney(discounted)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                )
            }
            else -> {
                Text(formatMoney(discounted), style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Text(
                    buildAnnotatedString {
                        append("antes ")
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(formatMoney(price))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun BottomActions(
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = formColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onBack,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(56.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colorResource(R.color.surface_secondary),
                contentColor = c.textPrimary,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = c.primary,
                contentColor = c.onPrimary,
                disabledContainerColor = c.inactive,
                disabledContentColor = c.onPrimary,
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = c.onPrimary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.RocketLaunch,
                        contentDescription = null,
                        tint = c.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Publicar oferta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun Pill(text: String, icon: ImageVector? = null) {
    val c = formColors()
    Surface(shape = RoundedCornerShape(50), color = c.primaryContainer, contentColor = c.primary) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    color: Color? = null,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val c = formColors()
    Text(
        buildAnnotatedString {
            append(text)
            if (required) withStyle(SpanStyle(color = c.primary)) { append(" *") }
        },
        style = style,
        color = color ?: c.textPrimary,
        modifier = modifier,
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: String? = null,
    suffix: String? = null,
    suffixColor: Color? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    containerColor: Color? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val c = formColors()
    val bg = containerColor ?: c.field
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = textStyle,
        placeholder = placeholder?.let { { Text(it, color = c.textSecondary) } },
        leadingIcon = leadingIcon,
        prefix = prefix?.let { { Text(it, color = c.textSecondary) } },
        suffix = suffix?.let {
            { Text(it, style = MaterialTheme.typography.labelLarge, color = suffixColor ?: c.textSecondary) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bg,
            unfocusedContainerColor = bg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = c.primary,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        ),
    )
}

private fun String.toPriceOrNull(): BigDecimal? = replace(",", ".").toBigDecimalOrNull()

private fun String.sanitizePrice(): String = filter { it.isDigit() || it == ',' || it == '.' }

private fun formatMoney(value: BigDecimal): String {
    val nf = NumberFormat.getNumberInstance(esAR).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "$" + nf.format(value)
}

private fun formatRelative(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val date = dateTime.toLocalDate()
    val day = when {
        date == today -> "Hoy"
        date == today.plusDays(1) -> "Mañana"
        date <= today.plusDays(6) ->
            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, esAR)
                .replaceFirstChar { it.uppercase() }
        else -> date.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
    return "$day ${dateTime.format(timeFormatter)}"
}

private fun ordinal(n: Int): String = when (n) {
    1, 3 -> "${n}ra"
    2 -> "2da"
    4, 5, 6 -> "${n}ta"
    7, 10 -> "${n}ma"
    8 -> "8va"
    9 -> "9na"
    else -> "${n}ª"
}