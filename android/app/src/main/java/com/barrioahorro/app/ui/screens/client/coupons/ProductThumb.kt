package com.barrioahorro.app.ui.screens.client.coupons

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.Primary

@Composable
fun ProductThumb(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    val url = photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(::resolvePhotoUrl)

    if (url == null) {
        InitialPlaceholder(name, size, modifier.size(size).clip(shape))
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
            loading = { InitialPlaceholder(name, size, Modifier.fillMaxSize()) },
            error = { state ->
                Log.w("ProductThumb", "No cargó $url", state.result.throwable)
                InitialPlaceholder(name, size, Modifier.fillMaxSize())
            },
        )
    }
}

@Composable
private fun InitialPlaceholder(name: String, size: Dp, modifier: Modifier) {
    val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "🎁"
    Box(modifier.background(BadgeOrange), contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            color = Primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

private fun resolvePhotoUrl(raw: String): String = when {
    raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
    else -> BASE_URL.trimEnd('/') + "/" + raw.trimStart('/')
}