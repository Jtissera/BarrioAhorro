package com.barrioahorro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BarrioAhorroColorScheme = lightColorScheme(
    // Marca
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Ink,

    secondary = Secondary,
    onSecondary = CardWhite,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Ink,

    tertiary = Tertiary,
    onTertiary = InkBlack,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Ink,

    // Fondos
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = WarmGray,
    surfaceTint = Primary,

    // Bordes
    outline = BorderGray,
    outlineVariant = TrackBeige,

    // Errores
    error = ErrorRed,
    onError = OnPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = Ink,
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
