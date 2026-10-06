package com.barrioahorro.app.ui.screens.business

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barrioahorro.app.ui.screens.onboarding.CategoryUi
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleRow

@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryUi>,
    selectedCategoryId: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text("Elegí el rubro") },
        text = {
            LazyColumn {
                items(categories, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = category.id == selectedCategoryId, onClick = { onSelect(category.id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category.name)
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBusinessProfileScreen(
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

    // El Scaffold de MainActivity ya aplica los insets del sistema; acá no los sumamos de nuevo.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.loadError != null -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = state.loadError, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetryLoad) { Text("Reintentar") }
            }

            else -> Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estos datos son los que ven tus vecinos en tu perfil.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Nombre del negocio", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.businessName,
                            onValueChange = onBusinessNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Rubro", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = { showCategoryPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
                                    ?: "Elegí un rubro",
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Dirección", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.direccion,
                            onValueChange = onDireccionChange,
                            placeholder = { Text("Av. Rivadavia 4520, Caballito") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { requestLocation() },
                            enabled = !state.isFetchingLocation,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                            } else {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Actualizar con mi ubicación actual")
                            }
                        }
                        if (state.latitud != null && state.longitud != null) {
                            Text(
                                text = "Ubicación: %.5f, %.5f".format(state.latitud, state.longitud),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Descripción", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.descripcion,
                            onValueChange = onDescripcionChange,
                            placeholder = { Text("Contales a tus vecinos qué los hace especial...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Horarios de atención", style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = onCopyMondayToAll) {
                                Text("Copiar lunes a todos")
                            }
                        }
                    }

                    items(state.schedule, key = { it.diaSemana }) { day ->
                        DayScheduleRow(
                            day = day,
                            onToggle = { onToggleDay(day.diaSemana) },
                            onAddSlot = { onAddSlot(day.diaSemana) },
                            onRemoveSlot = { slotId -> onRemoveSlot(day.diaSemana, slotId) },
                            onUpdateSlotStart = { slotId, time -> onUpdateSlotStart(day.diaSemana, slotId, time) },
                            onUpdateSlotEnd = { slotId, time -> onUpdateSlotEnd(day.diaSemana, slotId, time) },
                        )
                    }
                }

                state.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text("Guardar cambios")
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
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
