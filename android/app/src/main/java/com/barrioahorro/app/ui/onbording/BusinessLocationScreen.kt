
package com.barrioahorro.app.ui.screens.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.feedback.ErrorCard
import com.barrioahorro.app.ui.components.onboarding.OnboardingPrimaryButton
import com.barrioahorro.app.ui.components.onboarding.OnboardingTopBar
import com.barrioahorro.app.ui.components.onboarding.onboardingFieldColors
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
private const val CURRENT_STEP = 3

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
    alturaEncontrada: Boolean?,
    mensajeValidacion: String?,
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

    val primary = colorResource(R.color.primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val inactive = colorResource(R.color.neutral_inactive)

    val isBusy = isFetchingLocation || isValidatingAddress
    val hasPin = pinLatitud != null && pinLongitud != null

    val canContinue = hasPin &&
            direccion.isNotBlank() &&
            localidad.isNotBlank() &&
            !isBusy

    val hintMessage: String? = when {
        errorMessage == null &&
                hasPin &&
                !isBusy &&
                !isResolvingPinAddress &&
                (direccion.isBlank() || localidad.isBlank()) ->
            "Completá la dirección y la localidad de este punto para continuar."

        else -> null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            onRequestCurrentLocation()
        } else {
            onLocationPermissionDenied()
        }
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

    Scaffold(
        containerColor = colorResource(R.color.splash_background),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OnboardingTopBar(
                currentStep = CURRENT_STEP,
                onBack = onBack,
            )

            Spacer(modifier = Modifier.height(20.dp))

            PointOfSaleChip()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¿Dónde queda tu comercio?",
                style = MaterialTheme.typography.headlineMedium,
                color = textPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Esta dirección va a ser la que vean los vecinos en el mapa para encontrarte y retirar pedidos.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { requestLocation() },
                enabled = !isBusy,
                shape = CircleShape,
                border = BorderStroke(
                    1.5.dp,
                    if (isBusy) inactive else primary,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primary,
                    disabledContentColor = inactive,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isFetchingLocation) {
                    CircularProgressIndicator(
                        color = primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Usar mi ubicación actual",
                        style = MaterialTheme.typography.labelLarge,
                    )
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
                            CircularProgressIndicator(
                                color = primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else if (direccion.isNotBlank()) {
                            IconButton(
                                onClick = { onDireccionChange("") },
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Borrar dirección",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = onboardingFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
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
                    colors = onboardingFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onValidateAddress()
                        },
                    ),
                    modifier = Modifier.weight(2f),
                )
            }

            ErrorCard(title = errorMessage)

            hintMessage?.let { message ->
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Se reutiliza el espacio del aviso informativo existente.
            // La advertencia queda fuera del mapa y no agrega otro bloque
            // a la columna que reduzca su espacio disponible.
            if (
                alturaEncontrada == false &&
                !mensajeValidacion.isNullOrBlank()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp, max = 52.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Ubicación aproximada",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )

                            Text(
                                text = mensajeValidacion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2,
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(R.color.tertiary),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Podés editarla después desde tu perfil sin costo ni demoras.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NeighborhoodVisibilityCard()

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingPrimaryButton(
                text = "Siguiente",
                onClick = onContinue,
                enabled = canContinue,
            )
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

    val primary = colorResource(R.color.primary)
    val onPrimary = colorResource(R.color.on_primary)
    val surfaceCard = colorResource(R.color.surface_card)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    val currentOnPinMoved by rememberUpdatedState(onPinMoved)
    val currentPinLat by rememberUpdatedState(pinLatitud)
    val currentPinLng by rememberUpdatedState(pinLongitud)

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            zoomController.setVisibility(
                CustomZoomButtonsController.Visibility.ALWAYS,
            )

            minZoomLevel = 4.0
            maxZoomLevel = 19.0

            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(
                GeoPoint(DEFAULT_LAT, DEFAULT_LNG),
            )
        }
    }

    val lifecycleOwner = remember(context) {
        context.findLifecycleOwner()
    }

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

    DisposableEffect(mapView) {
        var lastTouchAt = 0L

        mapView.setOnTouchListener { _, _ ->
            lastTouchAt = SystemClock.uptimeMillis()
            false
        }

        fun notifyIfUserMoved() {
            val touchedRecently =
                SystemClock.uptimeMillis() - lastTouchAt < RECENT_TOUCH_MS

            if (!touchedRecently) return

            val center = mapView.mapCenter
            val lat = currentPinLat
            val lng = currentPinLng

            val sameAsPin = lat != null &&
                    lng != null &&
                    abs(center.latitude - lat) < COORD_EPSILON &&
                    abs(center.longitude - lng) < COORD_EPSILON

            if (!sameAsPin) {
                currentOnPinMoved(
                    center.latitude,
                    center.longitude,
                )
            }
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

    LaunchedEffect(pinLatitud, pinLongitud) {
        if (pinLatitud == null || pinLongitud == null) {
            return@LaunchedEffect
        }

        val center = mapView.mapCenter

        val yaEstaAhi =
            abs(center.latitude - pinLatitud) < COORD_EPSILON &&
                    abs(center.longitude - pinLongitud) < COORD_EPSILON

        if (!yaEstaAhi) {
            mapView.controller.setZoom(PIN_ZOOM)

            mapView.controller.animateTo(
                GeoPoint(pinLatitud, pinLongitud),
            )
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        if (hasPin) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-22).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = "Ubicación de tu comercio",
                    tint = onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primary),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            shape = CircleShape,
            color = surfaceCard,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = if (hasPin) {
                    "Ajustá el pin si hace falta"
                } else {
                    "Buscá tu dirección o mové el mapa"
                },
                style = MaterialTheme.typography.labelMedium,
                color = textPrimary,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp,
                ),
            )
        }

        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = textSecondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(surfaceCard.copy(alpha = 0.7f))
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp,
                ),
        )
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? =
    when (this) {
        is LifecycleOwner -> this
        is ContextWrapper -> baseContext.findLifecycleOwner()
        else -> null
    }

@Composable
private fun PointOfSaleChip() {
    val green = colorResource(R.color.secondary)

    Surface(
        shape = CircleShape,
        color = green.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Storefront,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = green,
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "TU PUNTO DE VENTA",
                style = MaterialTheme.typography.labelSmall,
                color = green,
            )
        }
    }
}

@Composable
private fun NeighborhoodVisibilityCard() {
    val green = colorResource(R.color.secondary)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = green.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorResource(R.color.surface_card)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = green,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Visibilidad en tu radio barrial",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorResource(R.color.text_primary),
                )

                Text(
                    text = "Los vecinos que viven cerca recibirán tus ofertas al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.text_secondary),
                )
            }
        }
    }
}
