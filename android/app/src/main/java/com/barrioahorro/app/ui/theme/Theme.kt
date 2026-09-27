package com.barrioahorro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BarrioAhorroColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = CardWhite,
    secondary = Rust,
    onSecondary = CardWhite,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = ToggleBg,
    onSurfaceVariant = WarmGray,
    outline = BorderGray,
    error = ErrorRed,
    onError = CardWhite,
)


@Composable
fun AndroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BarrioAhorroColorScheme,
        typography = Typography,
        content = content,
    )
}