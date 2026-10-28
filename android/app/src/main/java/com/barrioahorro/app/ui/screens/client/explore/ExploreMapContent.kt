package com.barrioahorro.app.ui.screens.client.explore

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.barrioahorro.app.data.explore.ExploreBusinessDto
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.theme.BadgeGreen
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.BadgeGreenText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.WarmGray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

private const val USER_COLOR = 0xFF1A73E8.toInt()

private fun zoomFor(distance: DistanceFilter): Double = when (distance) {
    DistanceFilter.UNDER_1 -> 15.0
    DistanceFilter.UNDER_3 -> 14.0
    DistanceFilter.ANY -> 13.0
}

@Composable
fun ExploreMapContent(
    state: ExploreUiState,
    onSelect: (Long?) -> Unit,
    onRetry: () -> Unit,
    onViewBusiness: (Long) -> Unit,
    onEditLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val location = state.location
    if (location == null) {
        NoLocationMap(onEditLocation, modifier)
        return
    }

    val selected = state.businesses.firstOrNull { it.id == state.selectedBusinessId }

    var cardHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier.fillMaxSize().clipToBounds()) {
        BusinessesMap(
            businesses = state.businesses,
            selected = selected,
            userLatitude = location.latitude,
            userLongitude = location.longitude,
            zoom = zoomFor(state.distance),
            onSelect = onSelect,
            bottomInset = if (selected != null) with(density) { cardHeightPx.toDp() } else 0.dp,
            modifier = Modifier.fillMaxSize(),
        )

        val statusText = when {
            state.isLoadingMap -> "Buscando comercios…"
            state.mapError != null -> state.mapError
            state.businesses.isEmpty() -> "No hay comercios con esos filtros"
            else -> null
        }
        if (statusText != null) {
            MapStatusPill(
                text = statusText,
                loading = state.isLoadingMap,
                onRetry = onRetry.takeIf { state.mapError != null },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
        }

        if (selected != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { cardHeightPx = it.height },
            ) {
                BusinessCard(
                    business = selected,
                    onViewBusiness = { onViewBusiness(selected.id) },
                )
            }
        }
    }
}

// ---- Mapa ------------------------------------------------------------------------

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun BusinessesMap(
    businesses: List<ExploreBusinessDto>,
    selected: ExploreBusinessDto?,
    userLatitude: Double,
    userLongitude: Double,
    zoom: Double,
    onSelect: (Long?) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSelect = rememberUpdatedState(onSelect)

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0,
            )
            minZoomLevel = 4.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(userLatitude, userLongitude))
            // El mapa se queda con todos los gestos: el padre no los intercepta.
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN,
                    android.view.MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Solo se vuelve a centrar si cambia la ubicación o el zoom (filtro de distancia), no con cada filtro.
    LaunchedEffect(userLatitude, userLongitude, zoom) {
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(GeoPoint(userLatitude, userLongitude))
    }

    LaunchedEffect(businesses, selected?.id, userLatitude, userLongitude) {
        mapView.render(
            context = context,
            businesses = businesses,
            selectedId = selected?.id,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            onSelect = { latestOnSelect.value(it) },
        )
    }

    LaunchedEffect(selected?.id) {
        selected?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
    }

    val buttonBottom by animateDpAsState(
        targetValue = bottomInset + 16.dp,
        label = "locateButtonBottom",
    )

    Box(modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().clipToBounds(),
        )

        Surface(
            onClick = { mapView.controller.animateTo(GeoPoint(userLatitude, userLongitude)) },
            shape = CircleShape,
            color = CardWhite,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = buttonBottom)
                .size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Centrar en mi ubicación", tint = Primary)
            }
        }
    }
}

private fun MapView.render(
    context: Context,
    businesses: List<ExploreBusinessDto>,
    selectedId: Long?,
    userLatitude: Double,
    userLongitude: Double,
    onSelect: (Long?) -> Unit,
) {
    overlays.clear()

    // Primero: recibe solo los toques que ningún pin consumió, para deseleccionar.
    overlays.add(
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    onSelect(null)
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            },
        ),
    )

    overlays.add(
        Marker(this).apply {
            position = GeoPoint(userLatitude, userLongitude)
            icon = circle(context, sizeDp = 18, fill = USER_COLOR)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
            setOnMarkerClickListener { _, _ -> true }
        },
    )

    businesses.sortedBy { it.id == selectedId }.forEach { business ->
        val isSelected = business.id == selectedId
        overlays.add(
            Marker(this).apply {
                position = GeoPoint(business.latitude, business.longitude)
                icon = pin(
                    context,
                    widthDp = if (isSelected) 46 else 34,
                    fill = when {
                        isSelected -> Ink.toArgb()
                        business.open -> Primary.toArgb()
                        else -> MutedLabel.toArgb()
                    },
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                    onSelect(business.id)
                    true
                }
            },
        )
    }
    invalidate()
}

private fun circle(context: Context, sizeDp: Int, fill: Int): GradientDrawable {
    val density = context.resources.displayMetrics.density
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke((3 * density).toInt(), 0xFFFFFFFF.toInt())
        setSize((sizeDp * density).toInt(), (sizeDp * density).toInt())
    }
}

// ---- Tarjeta y estados -----------------------------------------------------------

@Composable
private fun BusinessCard(
    business: ExploreBusinessDto,
    onViewBusiness: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().padding(12.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = SurfaceFieldAlt, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (business.photoUrl != null) {
                            AsyncImage(
                                model = business.photoUrl.toAbsolutePhotoUrl(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            )
                        } else {
                            Icon(Icons.Outlined.Image, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = business.name ?: "Comercio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        OpenPill(business.open)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        business.category?.takeIf { it.isNotBlank() }?.let {
                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceFieldAlt) {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGray,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Outlined.NearMe, contentDescription = null, tint = WarmGray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            ExploreOfferMapper.formatDistance(business.distanceMeters),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (business.activeOffers == 1) "1 oferta activa" else "${business.activeOffers} ofertas activas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                }
            }

            Button(
                onClick = onViewBusiness,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Ver comercio →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OpenPill(open: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = if (open) BadgeGreen else SurfaceFieldAlt) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).let { it }.then(Modifier), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = if (open) BadgeGreenDot else MutedLabel, modifier = Modifier.size(6.dp)) {}
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (open) "Abierto" else "Cerrado",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (open) BadgeGreenText else WarmGray,
            )
        }
    }
}

@Composable
private fun MapStatusPill(
    text: String,
    loading: Boolean,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(50), color = CardWhite, shadowElevation = 4.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = if (onRetry != null) 4.dp else 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = Ink)
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Reintentar", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NoLocationMap(onEditLocation: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MutedLabel, modifier = Modifier.size(48.dp))
        Text(
            text = "Activá tu ubicación para ver los comercios en el mapa.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onEditLocation,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
        ) {
            Text("Activar ubicación", fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.toAbsolutePhotoUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this else "${BASE_URL.trimEnd('/')}/${trimStart('/')}"


private fun pin(context: Context, widthDp: Int, fill: Int): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (widthDp * d)
    val h = w * 1.3f
    val shadow = 3 * d
    val bmp = Bitmap.createBitmap((w + shadow * 2).toInt(), (h + shadow * 2).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ox = shadow
    val oy = shadow / 2
    val r = w / 2f
    val cx = ox + r
    val cy = oy + r

    // Gota: círculo + punta
    val drop = Path().apply {
        addCircle(cx, cy, r - 1.5f * d, Path.Direction.CW)
        moveTo(cx - r * 0.62f, cy + r * 0.62f)
        lineTo(cx, oy + h - 1.5f * d)
        lineTo(cx + r * 0.62f, cy + r * 0.62f)
        close()
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0x33000000
    c.save(); c.translate(0f, 1.5f * d); c.drawPath(drop, paint); c.restore()
    paint.color = fill
    c.drawPath(drop, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * d
    paint.color = 0xFFFFFFFF.toInt()
    c.drawPath(drop, paint)

    // Tienda blanca: toldo + cuerpo
    paint.style = Paint.Style.FILL
    paint.color = 0xFFFFFFFF.toInt()
    val s = r * 0.52f
    c.drawRoundRect(RectF(cx - s, cy - s * 0.85f, cx + s, cy - s * 0.15f), 2 * d, 2 * d, paint)
    c.drawRect(RectF(cx - s * 0.8f, cy - s * 0.05f, cx + s * 0.8f, cy + s * 0.8f), paint)
    paint.color = fill
    c.drawRect(RectF(cx - s * 0.2f, cy + s * 0.25f, cx + s * 0.2f, cy + s * 0.8f), paint)

    return BitmapDrawable(context.resources, bmp)
}