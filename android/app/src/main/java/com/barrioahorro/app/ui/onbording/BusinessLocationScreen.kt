package com.barrioahorro.app.ui.screens.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File
import kotlin.math.abs

private const val DEFAULT_LAT = -34.6037
private const val DEFAULT_LNG = -58.3816
private const val DEFAULT_ZOOM = 11.0
private const val PIN_ZOOM = 17.0
private const val COORD_EPSILON = 1e-5
private const val RECENT_TOUCH_MS = 3000L

@Composable
fun BusinessLocationScreen(
    direccion: String,
    localidad: String,
    pinLatitud: Double?,
    pinLongitud: Double?,
    isFetchingLocation: Boolean,
    isValidatingAddress: Boolean,
    isResolvingPinAddress: Boolean,
    errorMessage: String?,
    onDireccionChange: (String) -> Unit,
    onLocalidadChange: (String) -> Unit,
    onValidateAddress: () -> Unit,
    onRequestCurrentLocation: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onPinMoved: (latitud: Double, longitud: Double) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isBusy = isFetchingLocation || isValidatingAddress
    val hasPin = pinLatitud != null && pinLongitud != null
    val canContinue = hasPin && direccion.isNotBlank() && localidad.isNotBlank() && !isBusy

    val helperMessage: String? = when {
        errorMessage != null -> errorMessage
        hasPin && !isBusy && !isResolvingPinAddress && (direccion.isBlank() || localidad.isBlank()) ->
            "Completá la dirección y la localidad de este punto para continuar."
        else -> null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onRequestCurrentLocation() else onLocationPermissionDenied()
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
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        StepHeader(currentStep = 3, totalSteps = 5, onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))
        PointOfSaleChip()
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "¿Dónde queda tu comercio?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Esta dirección va a ser la que vean los vecinos en el mapa para encontrarte y retirar pedidos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { requestLocation() },
            enabled = !isBusy,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isFetchingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = direccion,
                onValueChange = onDireccionChange,
                label = { Text("Calle y altura") },
                trailingIcon = {
                    if (isValidatingAddress || isResolvingPinAddress) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (direccion.isNotBlank()) {
                        IconButton(onClick = { onDireccionChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar dirección")
                        }
                    }
                },
                singleLine = true,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
                modifier = Modifier.weight(3f),
            )

            OutlinedTextField(
                value = localidad,
                onValueChange = onLocalidadChange,
                label = { Text("Localidad") },
                singleLine = true,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onValidateAddress()
                    },
                ),
                modifier = Modifier.weight(2f),
            )
        }

        helperMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LocationPickerMap(
            pinLatitud = pinLatitud,
            pinLongitud = pinLongitud,
            onPinMoved = onPinMoved,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 180.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Podés editarla después desde tu perfil sin costo ni demoras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        NeighborhoodVisibilityCard()
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            enabled = canContinue,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Siguiente")
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun LocationPickerMap(
    pinLatitud: Double?,
    pinLongitud: Double?,
    onPinMoved: (latitud: Double, longitud: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasPin = pinLatitud != null && pinLongitud != null

    val currentOnPinMoved by rememberUpdatedState(onPinMoved)
    val currentPinLat by rememberUpdatedState(pinLatitud)
    val currentPinLng by rememberUpdatedState(pinLongitud)

    val mapView = remember {
        // OpenStreetMap exige identificar la app; la caché va a cacheDir para no pedir permisos de almacenamiento.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LNG))
        }
    }

    // El MapView necesita enterarse del ciclo de vida de la pantalla.
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Cuando el vendedor termina de mover el mapa con el dedo, el centro es la nueva posición del pin.
    // Se ignoran los movimientos programáticos (animaciones al buscar o usar el GPS) mirando si hubo un toque reciente.
    DisposableEffect(mapView) {
        var lastTouchAt = 0L

        mapView.setOnTouchListener { _, _ ->
            lastTouchAt = SystemClock.uptimeMillis()
            false
        }

        fun notifyIfUserMoved() {
            val touchedRecently = SystemClock.uptimeMillis() - lastTouchAt < RECENT_TOUCH_MS
            if (!touchedRecently) return

            val center = mapView.mapCenter
            val lat = currentPinLat
            val lng = currentPinLng
            val sameAsPin = lat != null && lng != null &&
                    abs(center.latitude - lat) < COORD_EPSILON &&
                    abs(center.longitude - lng) < COORD_EPSILON
            if (!sameAsPin) currentOnPinMoved(center.latitude, center.longitude)
        }

        val listener = DelayedMapListener(
            object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    notifyIfUserMoved()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            },
            400L,
        )
        mapView.addMapListener(listener)

        onDispose {
            mapView.removeMapListener(listener)
            mapView.setOnTouchListener(null)
        }
    }

    // Georef o el GPS sugieren una posición: llevamos el mapa hasta ahí.
    // Si el mapa ya está en ese punto (porque el vendedor acaba de moverlo), no animamos.
    LaunchedEffect(pinLatitud, pinLongitud) {
        if (pinLatitud == null || pinLongitud == null) return@LaunchedEffect
        val center = mapView.mapCenter
        val yaEstaAhi = abs(center.latitude - pinLatitud) < COORD_EPSILON &&
                abs(center.longitude - pinLongitud) < COORD_EPSILON
        if (!yaEstaAhi) {
            mapView.controller.setZoom(PIN_ZOOM)
            mapView.controller.animateTo(GeoPoint(pinLatitud, pinLongitud))
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        if (hasPin) {
            // La base del círculo queda justo en el centro del mapa.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-22).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = "Ubicación de tu comercio",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = if (hasPin) "Ajustá el pin si hace falta" else "Buscá tu dirección o mové el mapa",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // Atribución obligatoria por la licencia de los datos de OpenStreetMap.
        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}

@Composable
private fun StepHeader(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun PointOfSaleChip() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Storefront,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TU PUNTO DE VENTA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun NeighborhoodVisibilityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Visibilidad en tu radio barrial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Los vecinos que viven cerca recibirán tus ofertas al instante.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}