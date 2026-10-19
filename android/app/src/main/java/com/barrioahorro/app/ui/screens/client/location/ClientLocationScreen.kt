package com.barrioahorro.app.ui.screens.client.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.domain.model.AddressCandidate
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
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
import com.barrioahorro.app.ui.theme.SecondaryContainer
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.TertiaryContainer
import com.barrioahorro.app.ui.theme.WarmGray
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

private const val MAP_ZOOM = 17.0
private val MAP_HEIGHT = 200.dp

@Composable
fun ClientLocationScreen(
    onFinished: () -> Unit,
    onClose: () -> Unit,
    viewModel: ClientLocationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { onFinished() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) viewModel.useGps() else viewModel.onPermissionDenied()
    }

    val requestGps = {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) {
            viewModel.useGps()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // Desde el paso de dirección (si se llegó por la intro), atrás vuelve a la intro.
    BackHandler(enabled = state.step == LocationStep.ADDRESS && !viewModel.isEditing) {
        viewModel.backToIntro()
    }

    when (state.step) {
        LocationStep.INTRO -> IntroContent(
            isFetching = state.isFetchingGps,
            onAllow = requestGps,
            onManual = viewModel::goToManual,
            onSkip = viewModel::skip,
        )

        LocationStep.ADDRESS -> AddressContent(
            state = state,
            onBack = { if (viewModel.isEditing) onClose() else viewModel.backToIntro() },
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onSearch = viewModel::search,
            onSelect = viewModel::selectCandidate,
            onPinMoved = viewModel::onPinMoved,
            onConfirm = viewModel::confirm,
        )
    }
}

// ---- Intro -----------------------------------------------------------------------

@Composable
private fun IntroContent(
    isFetching: Boolean,
    onAllow: () -> Unit,
    onManual: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceMuted.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(104.dp).clip(CircleShape).background(PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(32.dp))
                }
            }
        }

        Text(
            text = "Encontrá ofertas cerca tuyo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = InkBlack,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Usamos tu ubicación solo para mostrarte comercios y ofertas cercanas.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
        )

        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FeatureRow(Icons.Outlined.Storefront, PrimaryContainer, "Comercios a pasos de tu casa")
                FeatureRow(Icons.Outlined.NearMe, SecondaryContainer, "Ofertas ordenadas por distancia")
                FeatureRow(Icons.Outlined.Tune, TertiaryContainer, "Podés cambiarla cuando quieras")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onAllow,
            enabled = !isFetching,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            if (isFetching) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = OnPrimary)
            } else {
                Text("Permitir ubicación", fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = onManual,
            enabled = !isFetching,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceFieldAlt, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text("Ingresar dirección manualmente", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onSkip, enabled = !isFetching) {
            Text(
                text = "Si no la activás, igual podés usar la app",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

// ---- Dirección -------------------------------------------------------------------

@Composable
private fun AddressContent(
    state: ClientLocationUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onSelect: (AddressCandidate) -> Unit,
    onPinMoved: (Double, Double) -> Unit,
    onConfirm: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val hasError = state.error != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink)
            }
            Text(
                text = "Mi Ubicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.gpsFailed) {
                Surface(shape = RoundedCornerShape(14.dp), color = BadgeYellow, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BadgeYellowText)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "No pudimos obtener tu ubicación. Ingresá tu dirección.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BadgeYellowText,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "¿Dónde estás?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkBlack,
                )
                Text(
                    text = "Escribí tu dirección para ver comercios cerca.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                isError = hasError,
                placeholder = { Text("Ej.: Av. Hipólito Yrigoyen 4200, Lanús", color = WarmGray) },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Primary) },
                trailingIcon = {
                    when {
                        state.isSearching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        hasError -> Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorRed)
                        state.query.isNotEmpty() -> IconButton(onClick = onClear) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = WarmGray)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearch()
                }),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    errorContainerColor = FieldBackground,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = ErrorRed,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = ErrorRed)
            }

            if (state.candidates.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.candidates.forEachIndexed { index, candidate ->
                            CandidateRow(candidate, onClick = { onSelect(candidate) })
                            if (index < state.candidates.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceFieldAlt))
                            }
                        }
                    }
                }
            }

            if (state.hasPin) {
                PinAdjustMap(
                    latitude = state.pinLatitude!!,
                    longitude = state.pinLongitude!!,
                    onMoved = onPinMoved,
                )
                if (state.approximate) {
                    Text(
                        text = "No pudimos confirmar la altura: es una ubicación aproximada de la calle. Mové el pin para ajustarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                    )
                }
            } else {
                MapPlaceholder(isError = hasError)
            }

            if (hasError) {
                Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sugerencias útiles para buscar:", style = MaterialTheme.typography.labelMedium, color = Ink)
                        }
                        Text("Verificá que el número de altura sea correcto.", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                        Text(
                            "Escribí la localidad después de una coma: Av. Hipólito Yrigoyen 4200, Lanús.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = onConfirm,
                enabled = state.canConfirm,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = SurfaceMuted,
                    disabledContentColor = MutedLabel,
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = OnPrimary)
                } else {
                    Text("Confirmar ubicación", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: AddressCandidate, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(candidate.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Ink)
            val subtitle = listOfNotNull(
                candidate.locality,
                "Ubicación aproximada".takeIf { !candidate.exact },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGray)
            }
        }
    }
}

@Composable
private fun MapPlaceholder(isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceMuted.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(CardWhite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isError) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = if (isError) WarmGray else Primary,
                )
            }
            Text(
                text = if (isError) "Ubicación no delimitada" else "Buscá tu dirección para verla en el mapa",
                style = MaterialTheme.typography.labelMedium,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (isError) {
                Text(
                    text = "Asegurate de incluir calle y altura aproximada en tu barrio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

/** Mapa con el pin fijo al centro: el usuario mueve el mapa y, al soltar, se informa el nuevo centro. */
@SuppressLint("ClickableViewAccessibility")
@Composable
private fun PinAdjustMap(
    latitude: Double,
    longitude: Double,
    onMoved: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val latestOnMoved = rememberUpdatedState(onMoved)

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            controller.setZoom(MAP_ZOOM)
            controller.setCenter(GeoPoint(latitude, longitude))
            // El mapa está dentro de una columna que scrollea: se le da prioridad a los gestos del mapa.
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
            addMapListener(
                DelayedMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val center = mapCenter

                            if (center.latitude != 0.0 || center.longitude != 0.0) {
                                latestOnMoved.value(
                                    center.latitude,
                                    center.longitude,
                                )
                            }

                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean = false
                    },
                    400L,
                ),
            )
        }
    }

    // Si la ubicación cambia desde afuera (candidato elegido, nueva búsqueda), se centra el mapa.
    LaunchedEffect(latitude, longitude) {
        val center = mapView.mapCenter
        if (abs(center.latitude - latitude) > 1e-5 || abs(center.longitude - longitude) > 1e-5) {
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).offset(y = (-18).dp)) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Tu ubicación",
                tint = Primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Surface(
            shape = CircleShape,
            color = CardWhite.copy(alpha = 0.95f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        ) {
            Text(
                text = "Mové el mapa para ajustar el pin",
                style = MaterialTheme.typography.labelSmall,
                color = Ink,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}