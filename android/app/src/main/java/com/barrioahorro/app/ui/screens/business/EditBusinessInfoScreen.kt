package com.barrioahorro.app.ui.screens.business

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorContainer
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.ui.components.profile.AwningStripe
import com.barrioahorro.app.ui.components.profile.BottomActionSurface
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.FormFieldHeader
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.components.profile.ProfileTopBar
import com.barrioahorro.app.ui.components.profile.StatusChip
import com.barrioahorro.app.ui.screens.onboarding.CategoryUi

/** Soft limit shown in the description counter (display only, it is not enforced). */
private const val DESCRIPTION_MAX_CHARS = 200
private val fieldShape = RoundedCornerShape(12.dp)

@Composable
fun EditBusinessInfoScreen(
    state: EditBusinessProfileUiState,
    onBusinessNameChange: (String) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onDireccionChange: (String) -> Unit,
    onRequestCurrentLocation: () -> Unit,
    onDescripcionChange: (String) -> Unit,
    onToggleDay: (Int) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, String) -> Unit,
    onUpdateSlotStart: (Int, String, String) -> Unit,
    onUpdateSlotEnd: (Int, String, String) -> Unit,
    onCopyMondayToAll: () -> Unit,
    onRetryLoad: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    var showCategoryPicker by remember { mutableStateOf(false) }
    val background = Cream

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onRequestCurrentLocation()
    }

    fun requestLocation() {
        val hasPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        if (hasPermission) {
            onRequestCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    val showForm = !state.isLoading && state.loadError == null

    // MainActivity's Scaffold already applies the system insets; they are not added again here.
    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ProfileTopBar(title = "Editar información", onBack = onBack) },
        bottomBar = {
            if (showForm) {
                BottomActionSurface {
                    state.error?.let {
                        ErrorHint(text = it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PrimaryPillButton(
                        text = "Guardar cambios",
                        onClick = onSave,
                        icon = Icons.Outlined.CheckCircle,
                        enabled = !state.isSaving,
                        isLoading = state.isSaving,
                    )
                }
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }

            state.loadError != null -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorHint(text = state.loadError)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryPillButton(text = "Reintentar", onClick = onRetryLoad, height = 48.dp)
            }

            else -> EditProfileForm(
                state = state,
                contentPadding = paddingValues,
                onBusinessNameChange = onBusinessNameChange,
                onOpenCategoryPicker = { showCategoryPicker = true },
                onDireccionChange = onDireccionChange,
                onUseCurrentLocation = ::requestLocation,
                onDescripcionChange = onDescripcionChange,
                onToggleDay = onToggleDay,
                onAddSlot = onAddSlot,
                onRemoveSlot = onRemoveSlot,
                onUpdateSlotStart = onUpdateSlotStart,
                onUpdateSlotEnd = onUpdateSlotEnd,
                onCopyMondayToAll = onCopyMondayToAll,
            )
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onSelect = {
                onSelectCategory(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

@Composable
private fun EditProfileForm(
    state: EditBusinessProfileUiState,
    contentPadding: PaddingValues,
    onBusinessNameChange: (String) -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onDireccionChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onDescripcionChange: (String) -> Unit,
    onToggleDay: (Int) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, String) -> Unit,
    onUpdateSlotStart: (Int, String, String) -> Unit,
    onUpdateSlotEnd: (Int, String, String) -> Unit,
    onCopyMondayToAll: () -> Unit,
) {
    val textSecondary = WarmGray
    // Error state from the Figma "Error" frame: the save attempt failed and the address is still missing.
    val addressNeedsAttention = state.error != null && state.direccion.isBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { MerchantBanner(businessName = state.businessName) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormFieldHeader(label = "Nombre del comercio")
                OutlinedTextField(
                    value = state.businessName,
                    onValueChange = onBusinessNameChange,
                    singleLine = true,
                    shape = fieldShape,
                    colors = profileFieldColors(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormFieldHeader(label = "Rubro principal") {
                    Text("Obligatorio", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }
                CategorySelector(
                    categoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name,
                    onClick = onOpenCategoryPicker,
                )
            }
        }

        item {
            AddressField(
                direccion = state.direccion,
                hasLocation = state.latitud != null && state.longitud != null,
                needsAttention = addressNeedsAttention,
                latitud = state.latitud,
                longitud = state.longitud,
                isFetchingLocation = state.isFetchingLocation,
                onDireccionChange = onDireccionChange,
                onUseCurrentLocation = onUseCurrentLocation,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val length = state.descripcion.length
                FormFieldHeader(label = "Descripción del comercio") {
                    Text(
                        text = "$length/$DESCRIPTION_MAX_CHARS",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (length > DESCRIPTION_MAX_CHARS) ErrorRed else textSecondary,
                    )
                }
                OutlinedTextField(
                    value = state.descripcion,
                    onValueChange = onDescripcionChange,
                    placeholder = { Text("Contales a tus vecinos qué los hace especial...") },
                    minLines = 3,
                    shape = fieldShape,
                    colors = profileFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            BusinessScheduleCard(
                schedule = state.schedule,
                onToggleDay = onToggleDay,
                onAddSlot = onAddSlot,
                onRemoveSlot = onRemoveSlot,
                onUpdateSlotStart = onUpdateSlotStart,
                onUpdateSlotEnd = onUpdateSlotEnd,
                onCopyMondayToAll = onCopyMondayToAll,
            )
        }
    }
}

@Composable
private fun MerchantBanner(businessName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Cream,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = businessName.ifBlank { "Tu comercio" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Text(
                    text = "Modificá los datos para que tus vecinos te encuentren al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(categoryName: String?, onClick: () -> Unit) {
    Surface(
        shape = fieldShape,
        color = CardWhite,
        shadowElevation = 1.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).background(SecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Category,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = categoryName ?: "Elegí un rubro",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (categoryName != null) Ink else WarmGray,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Elegir rubro",
                tint = WarmGray,
            )
        }
    }
}

@Composable
private fun AddressField(
    direccion: String,
    hasLocation: Boolean,
    needsAttention: Boolean,
    latitud: Double?,
    longitud: Double?,
    isFetchingLocation: Boolean,
    onDireccionChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    val primary = Primary
    val secondary = Secondary

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormFieldHeader(label = "Dirección física") {
            when {
                needsAttention -> StatusChip(
                    text = "REQUIERE ATENCIÓN",
                    containerColor = ErrorContainer,
                    contentColor = ErrorRed,
                )

                hasLocation -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = secondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubicación visible", style = MaterialTheme.typography.bodySmall, color = secondary)
                }
            }
        }

        OutlinedTextField(
            value = direccion,
            onValueChange = onDireccionChange,
            placeholder = { Text("Av. Rivadavia 4520, Caballito") },
            singleLine = true,
            isError = needsAttention,
            shape = fieldShape,
            colors = profileFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            leadingIcon = {
                if (needsAttention) {
                    Box(
                        modifier = Modifier.size(28.dp).background(ErrorContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = primary)
                }
            },
            trailingIcon = when {
                needsAttention -> {
                    { Icon(Icons.Filled.Error, contentDescription = "Dirección incompleta", tint = ErrorRed) }
                }

                direccion.isNotEmpty() -> {
                    {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Cream)
                                .clickable { onDireccionChange("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Limpiar dirección",
                                tint = Ink,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                else -> null
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )

        if (needsAttention) {
            ErrorHint(
                text = "Ingresá la calle y altura de tu comercio o ubicalo con tu ubicación actual.",
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        BusinessAddressMap(
            latitud = latitud,
            longitud = longitud,
            isFetchingLocation = isFetchingLocation,
            onUseCurrentLocation = onUseCurrentLocation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<CategoryUi>,
    selectedCategoryId: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val textPrimary = Ink
    val primary = Primary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardWhite,
        scrimColor = textPrimary.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .background(Cream, CircleShape),
            )
        },
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp)) {
            Text(text = "Elegí el rubro", style = MaterialTheme.typography.titleLarge, color = textPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    val selected = category.id == selectedCategoryId
                    Surface(
                        shape = fieldShape,
                        color = if (selected) PrimaryContainer else Cream,
                        onClick = { onSelect(category.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** White, borderless field with a red focus ring: the input style of the edit-profile Figma frames. */
@Composable
private fun profileFieldColors(): TextFieldColors {
    val card = CardWhite
    val primary = Primary
    val textPrimary = Ink
    val textSecondary = WarmGray
    val error = ErrorRed

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = card,
        unfocusedContainerColor = card,
        errorContainerColor = card,
        focusedBorderColor = primary,
        unfocusedBorderColor = MutedLabel.copy(alpha = 0.25f),
        errorBorderColor = error,
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        errorTextColor = textPrimary,
        cursorColor = primary,
        errorCursorColor = primary,
        focusedPlaceholderColor = textSecondary,
        unfocusedPlaceholderColor = textSecondary,
        focusedLeadingIconColor = Color.Unspecified,
        unfocusedLeadingIconColor = Color.Unspecified,
        errorLeadingIconColor = Color.Unspecified,
    )
}
