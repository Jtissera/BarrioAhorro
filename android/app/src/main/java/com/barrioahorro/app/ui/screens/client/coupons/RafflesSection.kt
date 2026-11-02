package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

/** Puntos necesarios para desbloquear los sorteos. */
const val RAFFLES_UNLOCK_POINTS = 100

private val RaffleGold = Color(0xFFB8860B)
private val RaffleGoldPill = Color(0xFFFBE7B0)
private val RaffleGreenSoft = Color(0xFFDDF0E3)
private val RaffleGreenDark = Color(0xFF2E7D4F)
private val RaffleRedSoft = Color(0xFFFAD9D9)
private val RaffleShackle = Color(0xFFE9DDD0)
private val RaffleGlow = Color(0xFFFFE3D3)

internal enum class CouponsSection { CUPONES, SORTEOS }

// ---- Selector Cupones | Sorteos ---------------------------------------------------

@Composable
internal fun CouponsSectionToggle(
    selected: CouponsSection,
    onSelect: (CouponsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color(0xFFFAEFD8), RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleItem("Cupones", Icons.Default.ConfirmationNumber, selected == CouponsSection.CUPONES, Modifier.weight(1f)) {
            onSelect(CouponsSection.CUPONES)
        }
        ToggleItem("Sorteos", Icons.Default.Star, selected == CouponsSection.SORTEOS, Modifier.weight(1f)) {
            onSelect(CouponsSection.SORTEOS)
        }
    }
}

@Composable
private fun ToggleItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fg = if (selected) Color.White else InkBlack
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ---- Sección Sorteos --------------------------------------------------------------

@Composable
internal fun RafflesSection(
    points: Int?,
    onGoToForum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlocked = points != null && points >= RAFFLES_UNLOCK_POINTS
    val missing = points?.let { (RAFFLES_UNLOCK_POINTS - it).coerceAtLeast(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sorteos", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = InkBlack)
                Text("Beneficios y premios del barrio", fontSize = 12.sp, color = WarmGray)
            }
            Surface(shape = RoundedCornerShape(50), color = RaffleGoldPill) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (points != null) "$points puntos" else "— puntos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                    )
                }
            }
        }

        Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Borde superior que se desvanece, como en el mock.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(Brush.verticalGradient(listOf(Primary.copy(alpha = 0.75f), Color.Transparent))),
                )
                LockIllustration(unlocked = unlocked)
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (unlocked) UnlockedContent() else LockedContent(missing = missing)
                }
            }
        }

        HowToEarnFaster(onClick = onGoToForum)
    }
}

@Composable
private fun LockedContent(missing: Int?) {
    Text(
        "Sorteos y premios especiales",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = InkBlack,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Surface(shape = RoundedCornerShape(50), color = RaffleRedSoft, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (missing != null) "Te faltan $missing puntos para desbloquearlos."
                else "Sumá $RAFFLES_UNLOCK_POINTS puntos para desbloquearlos.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(Modifier.size(4.dp).background(Color(0xFFD9CFC2), CircleShape)) }
    }
    Spacer(Modifier.height(14.dp))
    Text(
        buildAnnotatedString {
            append("Se desbloquean al llegar a ")
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$RAFFLES_UNLOCK_POINTS puntos") }
            append(". Llegar a $RAFFLES_UNLOCK_POINTS no descuenta tus puntos.")
        },
        fontSize = 13.sp,
        color = InkBlack,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = RaffleGreenDark, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text("Ganás puntos con los upvotes que recibís.", fontSize = 12.sp, color = WarmGray)
    }
}

/** Estado desbloqueado: placeholder hasta tener el listado real de sorteos. */
@Composable
private fun UnlockedContent() {
    Text(
        "¡Sorteos desbloqueados!",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = InkBlack,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Muy pronto vas a poder participar de los premios del barrio.",
        fontSize = 13.sp,
        color = WarmGray,
        textAlign = TextAlign.Center,
    )
}

/** Candado con brillo radial detrás y la llavecita naranja. */
@Composable
private fun LockIllustration(unlocked: Boolean) {
    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        // Brillo
        Box(
            Modifier
                .size(210.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RaffleGlow, RaffleGlow.copy(alpha = 0.35f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Canvas(Modifier.size(width = 96.dp, height = 112.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 13.dp.toPx()
            val arcW = w * 0.56f
            val left = (w - arcW) / 2f
            val bodyTop = h * 0.40f

            // Arco: abierto (girado) si está desbloqueado, cerrado si no.
            val shackleLeft = if (unlocked) left + 10.dp.toPx() else left
            drawArc(
                color = RaffleShackle,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(shackleLeft, stroke / 2f),
                size = Size(arcW, arcW),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            val legTop = stroke / 2f + arcW / 2f
            drawLine(RaffleShackle, Offset(shackleLeft, legTop), Offset(shackleLeft, bodyTop + 4.dp.toPx()), stroke)
            drawLine(
                RaffleShackle,
                Offset(shackleLeft + arcW, legTop),
                Offset(shackleLeft + arcW, if (unlocked) legTop + 10.dp.toPx() else bodyTop + 4.dp.toPx()),
                stroke,
            )

            // Cuerpo
            drawRoundRect(
                color = Primary,
                topLeft = Offset(0f, bodyTop),
                size = Size(w, h - bodyTop),
                cornerRadius = CornerRadius(20.dp.toPx()),
            )
            // Ojo de cerradura
            val cx = w / 2f
            val cy = bodyTop + (h - bodyTop) * 0.42f
            drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(cx, cy))
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - 2.5.dp.toPx(), cy),
                size = Size(5.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }

        // Llavecita
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 120.dp)
                .size(34.dp)
                .background(BadgeOrange, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF7A4B00), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HowToEarnFaster(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(RaffleGreenSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Forum, contentDescription = null, tint = RaffleGreenDark, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("¿Cómo sumar más rápido?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
            Text(
                "Compartí ofertas y aportá datos útiles a tus vecinos en el foro.",
                fontSize = 12.sp,
                color = WarmGray,
            )
        }
    }
}