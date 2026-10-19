package com.barrioahorro.app.ui.screens.offers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BadgeGreenDot
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.TertiaryContainer
import com.barrioahorro.app.ui.theme.WarmGray
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OfferPublishedScreen(
    state: CreateOfferUiState,
    onBackToOffers: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Cream,
            topBar = {
                ToldoConFranjas(
                    modifier = Modifier
                        .background(Cream)
                        .statusBarsPadding(),
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = onBackToOffers,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver mis ofertas", fontWeight = FontWeight.Bold)
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SuccessBadge()
                Text(
                    text = "¡Tu oferta ya está publicada!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Tus vecinos ya pueden verla en el feed y en el mapa del barrio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                OfferPreview(
                    state = state,
                    price = state.priceValue,
                    isLastUnit = state.selectedType == OfferType.POR_CANTIDAD,
                    isTwoForOne = state.selectedType == OfferType.DOS_POR_UNO,
                    isWholesale = state.selectedType == OfferType.POR_MAYOR,
                )
            }
        }

        ConfettiOverlay(Modifier.fillMaxSize())
    }
}

@Composable
private fun SuccessBadge() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = -6f
            }
            .background(Secondary, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(54.dp))
    }
}

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

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
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