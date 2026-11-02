package com.barrioahorro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.TertiaryContainer
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val drift: Float,
    val sizeDp: Float,
    val rotation: Float,
    val spin: Float,
    val color: Color,
)

/** Confeti de una sola pasada (4 s). No intercepta toques. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val colors = remember { listOf(Primary, Tertiary, Secondary, TertiaryContainer, BadgeGreenDot) }
    val pieces = remember {
        val rnd = Random(7)
        List(90) {
            ConfettiPiece(
                x = rnd.nextFloat(),
                delay = rnd.nextFloat() * 0.35f,
                speed = 0.7f + rnd.nextFloat() * 0.8f,
                drift = (rnd.nextFloat() - 0.5f) * 0.25f,
                sizeDp = 6f + rnd.nextFloat() * 6f,
                rotation = rnd.nextFloat() * 360f,
                spin = (rnd.nextFloat() - 0.5f) * 900f,
                color = colors[rnd.nextInt(colors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 4000, easing = LinearEasing))
        finished = true
    }
    if (finished) return

    Canvas(modifier) {
        val t = progress.value
        pieces.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach

            val y = (-0.05f + local * p.speed * 1.2f) * size.height
            val x = (p.x + p.drift * local + 0.03f * sin(local * 12f + p.rotation)) * size.width
            val alpha = if (local > 0.8f) (1f - local) / 0.2f else 1f
            val s = p.sizeDp.dp.toPx()

            rotate(degrees = p.rotation + p.spin * local, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - s / 2f, y - s / 3f),
                    size = Size(s, s * 0.6f),
                )
            }
        }
    }
}