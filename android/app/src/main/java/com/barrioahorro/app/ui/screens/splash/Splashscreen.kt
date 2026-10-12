package com.barrioahorro.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import kotlinx.coroutines.flow.first

// Colores muestreados del mockup A1 (captura, así que pueden diferir 1-2 niveles del original).
// Reemplazar por los del tema cuando estén definidos.
// El crema NO está acá: vive en res/values/splash.xml (splash_background) para que el splash
// del sistema y este tengan exactamente el mismo fondo y la transición no se note.
private val SplashRed = Color(0xFFD42E12)
private val SplashBrown = Color(0xFF2F1A11)

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private const val STRIPE_COUNT = 14      // 7 rojas + 7 de fondo, como en el mockup
private const val STRIPE_STAGGER = 4f
private const val FADE_HALF_WIDTH = 3f   // en rayas; el mockup aclara las del centro

/**
 * Splash en Compose (mockup A1). Toda la secuencia corre sobre una única línea de tiempo 0..1
 * de [minDisplayMillis]; cada elemento anima en un tramo de esa línea:
 *
 *   0.00–0.40  rayas del toldo "caen" de izquierda a derecha
 *   0.15–0.55  logo: fade + sube + escala con rebote leve
 *   0.50–0.75  eslogan: fade + sube
 *   0.00–1.00  barra de progreso inferior
 *
 * Llama a [onFinished] cuando terminó la animación Y [isReady] es true. Así, más adelante,
 * el ViewModel puede pasar isReady = false mientras resuelve la sesión sin tocar esta pantalla.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isReady: Boolean = true,
    minDisplayMillis: Int = 1800,
) {
    val timeline = remember { Animatable(0f) }
    val latestOnFinished by rememberUpdatedState(onFinished)
    val latestIsReady by rememberUpdatedState(isReady)

    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = minDisplayMillis, easing = LinearEasing),
        )
        snapshotFlow { latestIsReady }.first { it }
        latestOnFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.splash_background)),
    ) {
        AwningStripes(
            progress = { segment(timeline.value, 0f, 0.4f) },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_barrio_ahorro),
                contentDescription = stringResource(R.string.splash_logo_description),
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .graphicsLayer {
                        val t = timeline.value
                        val pop = segment(t, 0.15f, 0.55f, EaseOutBack)
                        alpha = segment(t, 0.15f, 0.40f, LinearEasing)
                        scaleX = 0.8f + 0.2f * pop
                        scaleY = 0.8f + 0.2f * pop
                        translationY = (1f - pop) * 24.dp.toPx()
                    },
            )

            Spacer(Modifier.height(56.dp))

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = SplashBrown,
                modifier = Modifier.graphicsLayer {
                    val p = segment(timeline.value, 0.5f, 0.75f)
                    alpha = p
                    translationY = (1f - p) * 8.dp.toPx()
                },
            )
        }

        ProgressBar(
            progress = { timeline.value },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        )
    }
}


@Composable
private fun AwningStripes(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val stripeWidth = size.width / STRIPE_COUNT
        val p = progress()
        for (i in 0 until STRIPE_COUNT step 2) {
            val local = ((p * (STRIPE_COUNT + STRIPE_STAGGER) - i) / STRIPE_STAGGER).coerceIn(0f, 1f)
            val distFromCenter = kotlin.math.abs(i + 0.5f - STRIPE_COUNT / 2f)
            drawRect(
                color = SplashRed.copy(alpha = 0.35f + 0.65f),
                topLeft = Offset(i * stripeWidth, 0f),
                size = Size(stripeWidth, size.height * local),
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth(0.5f)
            .height(4.dp),
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = SplashRed.copy(alpha = 0.18f), cornerRadius = radius)
        drawRoundRect(
            color = SplashRed,
            size = Size(size.width * progress(), size.height),
            cornerRadius = radius,
        )
    }
}

/** Mapea [t] (0..1 global) al tramo [start]..[end], normalizado a 0..1 y con easing. */
private fun segment(
    t: Float,
    start: Float,
    end: Float,
    easing: Easing = FastOutSlowInEasing,
): Float = easing.transform(((t - start) / (end - start)).coerceIn(0f, 1f))