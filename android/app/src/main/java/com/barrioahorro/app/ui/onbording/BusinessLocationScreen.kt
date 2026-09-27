package com.barrioahorro.app.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BusinessLocationScreen(
    direccion: String,
    latitud: Double?,
    longitud: Double?,
    isFetchingLocation: Boolean,
    errorMessage: String?,
    onDireccionChange: (String) -> Unit,
    onRequestCurrentLocation: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onRequestCurrentLocation()
    }

    fun requestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            onRequestCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Paso 3 de 4", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "¿Dónde te encuentran?", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Así los vecinos de tu barrio pueden ubicarte en el mapa.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Dirección", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = direccion,
            onValueChange = onDireccionChange,
            placeholder = { Text("Av. Rivadavia 4520, Caballito") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { requestLocation() },
            enabled = !isFetchingLocation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isFetchingLocation) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
            } else {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (latitud != null && longitud != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.height(40.dp).width(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ubicación capturada",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "%.5f, %.5f".format(latitud, longitud),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Text(
                    text = "Todavía no capturaste tu ubicación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.width(56.dp)) {
                Text("<")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onContinue,
                enabled = direccion.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Continuar")
            }
        }
    }
}