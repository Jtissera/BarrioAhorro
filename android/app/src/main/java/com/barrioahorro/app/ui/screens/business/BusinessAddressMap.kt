package com.barrioahorro.app.ui.screens.business

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceMuted
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File

private const val PREVIEW_ZOOM = 16.0
private val MAP_HEIGHT = 140.dp

/**
 * Read-only map card under the address field. With coordinates it shows the pinned location;
 * without them it shows the "Punto no reconocido" alert from the error state.
 * The pill always lets the merchant refresh the location with the device GPS.
 */
@Composable
fun BusinessAddressMap(
    latitud: Double?,
    longitud: Double?,
    isFetchingLocation: Boolean,
    onUseCurrentLocation: () -> Unit,
    showLocationPill: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val hasLocation = latitud != null && longitud != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .shadow(1.dp, shape)
            .clip(shape)
            .background(SurfaceMuted),
    ) {
        if (hasLocation) {
            ReadOnlyMap(latitud = latitud!!, longitud = longitud!!, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Ink.copy(alpha = 0.1f)))
            MapPin(modifier = Modifier.align(Alignment.Center).offset(y = (-8).dp))
        } else {
            UnrecognizedPointOverlay(modifier = Modifier.fillMaxSize())
        }
        if (showLocationPill) {
            UseLocationPill(
                isFetching = isFetchingLocation,
                onClick = onUseCurrentLocation,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            )
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun ReadOnlyMap(latitud: Double, longitud: Double, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        // OpenStreetMap requires an app user agent; the cache lives in cacheDir so no storage permission is needed.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            // Preview only: swallow touches so the map never fights the form's scroll.
            setOnTouchListener { _, _ -> true }
            controller.setZoom(PREVIEW_ZOOM)
        }
    }

    LaunchedEffect(latitud, longitud) {
        mapView.controller.setCenter(GeoPoint(latitud, longitud))
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

@Composable
private fun MapPin(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(6.dp, CircleShape)
                .background(Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Ubicación del comercio",
                tint = OnPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(6.dp)
                .background(Ink.copy(alpha = 0.4f), CircleShape),
        )
    }
}

@Composable
private fun UnrecognizedPointOverlay(modifier: Modifier = Modifier) {
    val alertRed = Color(0xFFAD1800)
    Column(
        modifier = modifier
            .background(Cream)
            .background(alertRed.copy(alpha = 0.1f))
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(44.dp).background(alertRed.copy(alpha = 0.3f), CircleShape))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocationOff,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Punto no reconocido",
            style = MaterialTheme.typography.labelSmall,
            color = SurfaceSecondary,
            modifier = Modifier
                .background(Color(0xE6352F29), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UseLocationPill(isFetching: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = CardWhite.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = modifier.clip(CircleShape).clickable(enabled = !isFetching, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(12.dp),
                )
            } else {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(12.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Usar mi ubicación actual",
                style = MaterialTheme.typography.labelSmall,
                color = Ink,
            )
        }
    }
}
