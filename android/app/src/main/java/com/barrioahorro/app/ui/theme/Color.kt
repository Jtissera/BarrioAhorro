package com.barrioahorro.app.ui.theme

import androidx.compose.ui.graphics.Color

// Marca BarrioAhorro
val Primary = Color(0xFFD42E12)
val PrimaryPressed = Color(0xFFA8240D)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFFDE8E3)

val Secondary = Color(0xFF2F6B55)
val SecondaryContainer = Color(0xFFBFEFD8)
val Tertiary = Color(0xFFF2B134)
val TertiaryContainer = Color(0xFFFFDEAC)

// Fondos
val Cream = Color(0xFFFBF6E6)
val CardWhite = Color(0xFFFFFFFF)
val SurfaceSecondary = Color(0xFFFFF9EC)
val SurfaceMuted = Color(0xFFE3D8CE)
val SurfaceBottomBar = Color(0xFFFFF8F4)

// Textos
val Ink = Color(0xFF2E1F17)
val InkBlack = Color(0xFF000000)
val WarmGray = Color(0xFF6B5A50)

// Bordes y elementos inactivos
val MutedLabel = Color(0xFFB9AFA6)
val PlaceholderGray = Color(0xFFB9AFA6)
val BorderGray = Color(0xFFD9CDBB)
val TrackBeige = Color(0xFFE5BEB6)
val ToggleBg = Color(0xFFF7ECE2)

// Estados
val ErrorRed = Color(0xFFB3261E)
val ErrorContainer = Color(0xFFFFDAD6)
val WarningAmber = Color(0xFFF2B134)
val SuccessGreen = Color(0xFF2F6B55)

// Formularios y horarios
val FieldBackground = Color(0xFFFFFFFF)
val SurfaceDay = Color(0xFFFDF2E8)
val SurfaceDayClosed = Color(0xFFF7ECE2)
val SurfaceFieldAlt = Color(0xFFF3ECE4)

// Onboarding
val StepperCream = Color(0xFFFBF6E6)
val StepperBorder = Color(0xFFD9CDBB)

// Badges y Tags (ofertas, estados, tags de comercio)
val BadgeOrange = Color(0xFFFFE0D2)
val BadgeOrangeBorder = Color(0xFFFFD9C7)
val BadgeOrangeLight = Color(0xFFFFF2EB)
val BadgeYellow = Color(0xFFFFECC4)
val BadgeYellowText = Color(0xFF8A5300)
val BadgeGreen = Color(0xFFD2F5DC)
val BadgeGreenDot = Color(0xFF1E824C)
val BadgeGreenText = Color(0xFF1B5E39)
val BadgeGreenSoft = Color(0xFFE8F7F0)
val BadgeGreenSoftBorder = Color(0xFFBCE8D3)
val BadgeGreenDark = Color(0xFF2E7D32)
val HighlightOrange = Color(0xFFFFF0EC)
val HighlightOrangeBorder = Color(0xFFFFD6CC)
val ForestGreen = Color(0xFF2C6B4F)

// Aliases semánticos para unificación total
val SplashBackground = Cream
val SurfaceCard = CardWhite
val TextPrimary = Ink
val TextSecondary = WarmGray
val NeutralInactive = MutedLabel
val OutlineVariant = TrackBeige
val TitleBlack = InkBlack
val Error = ErrorRed

// Compatibilidad con las referencias existentes
object AppColors {
    val mutedLabel = MutedLabel
    val placeholder = PlaceholderGray
    val border = BorderGray
    val track = TrackBeige
    val toggleBackground = ToggleBg
    val warning = WarningAmber
    val success = SuccessGreen
}