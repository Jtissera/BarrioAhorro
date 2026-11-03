package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.components.ConfettiOverlay
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import kotlinx.coroutines.delay

private val RaffleGold = Color(0xFFB8860B)
private val RaffleGoldPill = Color(0xFFFBE7B0)
private val RaffleGreenSoft = Color(0xFFDDF0E3)
private val RaffleGreenDark = Color(0xFF2E7D4F)
private val RaffleRedSoft = Color(0xFFFAD9D9)
private val RaffleShackle = Color(0xFFE9DDD0)
private val RaffleGlow = Color(0xFFFFE3D3)
private val RaffleDisabled = Color(0xFFE6DDD0)
private val RaffleNote = Color(0xFFFAF1DF)
private val RaffleDivider = Color(0xFFF0E8DA)

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
    icon: ImageVector,
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
    onGoToForum: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RafflesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    // Mientras suena la animación se muestra la vista bloqueada aunque ya esté desbloqueado.
    val showLocked = !state.unlocked || state.playUnlockAnimation

    Box(modifier.fillMaxSize()) {
        when {
            !state.loaded && state.error != null -> RaffleStatePanel(
                title = "No pudimos cargar los sorteos",
                subtitle = state.error.orEmpty(),
                modifier = Modifier.padding(16.dp),
            ) {
                Button(
                    onClick = viewModel::refresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(50),
                ) { Text("Reintentar") }
            }

            !state.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
            }

            else -> AnimatedContent(
                targetState = showLocked,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "raffles",
            ) { locked ->
                if (locked) {
                    LockedView(
                        state = state,
                        onOpened = { confetti = true },
                        onUnlockFinished = viewModel::onUnlockAnimationFinished,
                        onGoToForum = onGoToForum,
                    )
                } else {
                    UnlockedView(
                        state = state,
                        onBuy = viewModel::requestBuy,
                        onMyTickets = viewModel::onMyTicketsClick,
                    )
                }
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        state.confirming?.let { raffle ->
            BuyTicketSheet(
                raffle = raffle,
                balance = state.balance,
                isLoading = state.isBuying,
                onConfirm = viewModel::confirmBuy,
                onDismiss = viewModel::dismissBuy,
            )
        }

        if (confetti) ConfettiOverlay(Modifier.fillMaxSize())
    }
}

// ---- Vista bloqueada (y animación de desbloqueo) ----------------------------------

@Composable
private fun LockedView(
    state: RafflesUiState,
    onOpened: () -> Unit,
    onUnlockFinished: () -> Unit,
    onGoToForum: () -> Unit,
) {
    val animating = state.playUnlockAnimation
    val progress = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    var opened by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val currentOnOpened by rememberUpdatedState(onOpened)
    val currentOnFinished by rememberUpdatedState(onUnlockFinished)

    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        delay(700)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        opened = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        currentOnOpened()
        pulse.animateTo(1.12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        delay(1500)
        currentOnFinished()
    }

    Column(
        modifier = Modifier
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
            PointsChip(state.balance)
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
                LockIllustration(open = progress.value, pulse = pulse.value)
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (animating) {
                        UnlockingContent(opened = opened, threshold = state.threshold)
                    } else {
                        LockedContent(missing = state.missing, threshold = state.threshold)
                    }
                }
            }
        }

        if (!animating) HowToEarnFaster(onClick = onGoToForum)
    }
}

@Composable
private fun PointsChip(points: Int) {
    Surface(shape = RoundedCornerShape(50), color = RaffleGoldPill) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("$points puntos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        }
    }
}

@Composable
private fun LockedContent(missing: Int, threshold: Int) {
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
                "Te faltan $missing puntos para desbloquearlos.",
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
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$threshold puntos") }
            append(". Llegar a $threshold no descuenta tus puntos.")
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

@Composable
private fun UnlockingContent(opened: Boolean, threshold: Int) {
    Text(
        if (opened) "¡Sorteos desbloqueados!" else "¡Llegaste a $threshold puntos!",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = InkBlack,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Llegar a $threshold no descontó tus puntos.",
        fontSize = 13.sp,
        color = WarmGray,
        textAlign = TextAlign.Center,
    )
}

/**
 * open: 0 = cerrado, 1 = abierto. Al abrir, el arco sube y gira sobre su pata izquierda,
 * la pata derecha se libera y la llavecita se desvanece.
 */
@Composable
private fun LockIllustration(open: Float, pulse: Float) {
    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        // Brillo
        Box(
            Modifier
                .size(210.dp)
                .graphicsLayer {
                    alpha = 0.7f + 0.3f * open
                    val s = 1f + 0.15f * open
                    scaleX = s
                    scaleY = s
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(RaffleGlow, RaffleGlow.copy(alpha = 0.35f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Canvas(
            Modifier
                .size(width = 96.dp, height = 112.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
        ) {
            val w = size.width
            val h = size.height
            val stroke = 13.dp.toPx()
            val arcW = w * 0.56f
            val left = (w - arcW) / 2f
            val bodyTop = h * 0.40f
            val legTop = stroke / 2f + arcW / 2f
            val lift = 16.dp.toPx() * open
            val rightLegBottom = lerp(bodyTop + 4.dp.toPx(), legTop + 10.dp.toPx(), open)

            // Arco: se dibuja primero para que el cuerpo tape el extremo de las patas.
            withTransform({
                translate(left = 0f, top = -lift)
                rotate(degrees = -24f * open, pivot = Offset(left, bodyTop))
            }) {
                drawArc(
                    color = RaffleShackle,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(left, stroke / 2f),
                    size = Size(arcW, arcW),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                drawLine(RaffleShackle, Offset(left, legTop), Offset(left, bodyTop + 4.dp.toPx()), stroke)
                drawLine(RaffleShackle, Offset(left + arcW, legTop), Offset(left + arcW, rightLegBottom), stroke)
            }

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

        // Llavecita: gira y se desvanece al abrir
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 120.dp)
                .size(34.dp)
                .graphicsLayer {
                    alpha = 1f - open
                    rotationZ = 360f * open
                }
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

// ---- Vista desbloqueada -----------------------------------------------------------

@Composable
private fun UnlockedView(
    state: RafflesUiState,
    onBuy: (RaffleUi) -> Unit,
    onMyTickets: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sorteos",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkBlack,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = RoundedCornerShape(50), color = RaffleGreenSoft) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(RaffleGreenDark, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text("Activos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaffleGreenDark)
                    }
                }
            }
        }

        item { BalanceCard(balance = state.balance, onMyTickets = onMyTickets) }

        if (state.raffles.isEmpty()) {
            item {
                RaffleStatePanel(
                    title = "Todavía no hay sorteos activos",
                    subtitle = "Volvé pronto, los comercios del barrio van sumando premios.",
                )
            }
        } else {
            items(state.raffles, key = { it.id }) { raffle ->
                RaffleCard(raffle = raffle, balance = state.balance, onBuy = { onBuy(raffle) })
            }
        }

        item { DrawNote() }
    }
}

@Composable
private fun BalanceCard(balance: Int, onMyTickets: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp).background(RaffleGoldPill, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("TU SALDO DISPONIBLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                Text("$balance puntos", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = InkBlack)
            }
            TextButton(onClick = onMyTickets) {
                Text("Mis tickets →", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RaffleCard(raffle: RaffleUi, balance: Int, onBuy: () -> Unit) {
    val missing = (raffle.ticketCost - balance).coerceAtLeast(0)

    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = BadgeOrange, modifier = Modifier.weight(1f, fill = false)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Aporta ${raffle.businessName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(50), color = RaffleGoldPill) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${raffle.ticketCost} pts por ticket", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                raffle.prizeName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = WarmGray, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Se sortea el ${raffle.drawDate}", fontSize = 12.sp, color = WarmGray)
            }

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(RaffleDivider))
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append("Probabilidad actual: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = InkBlack)) { append("1 en ${raffle.oneIn}") }
                    },
                    fontSize = 12.sp,
                    color = WarmGray,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.Group, contentDescription = null, tint = RaffleGreenDark, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${raffle.participants} participantes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RaffleGreenDark)
            }

            if (raffle.myTickets > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (raffle.myTickets == 1) "Ya tenés 1 ticket" else "Ya tenés ${raffle.myTickets} tickets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                )
            }

            Spacer(Modifier.height(12.dp))
            if (missing == 0) {
                Button(
                    onClick = onBuy,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Comprar ticket", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Surface(shape = RoundedCornerShape(50), color = RaffleDisabled, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Te faltan $missing pts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawNote() {
    Surface(shape = RoundedCornerShape(14.dp), color = RaffleNote, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(RaffleGoldPill, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Los sorteos se realizan con el bolillero digital comunitario los últimos días de cada mes. ¡Mucha suerte, vecino!",
                fontSize = 12.sp,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun RaffleStatePanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFDF3DC), modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFFFAD9CF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = WarmGray, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                action()
            }
        }
    }
}

// ---- Confirmación de compra -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyTicketSheet(
    raffle: RaffleUi,
    balance: Int,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardWhite) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirmar compra", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = InkBlack)

            Surface(shape = RoundedCornerShape(14.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(photoUrl = raffle.photoUrl, name = raffle.prizeName, size = 60.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Aporta ${raffle.businessName}", fontSize = 12.sp, color = WarmGray)
                        Text(raffle.prizeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkBlack)
                        Text("Se sortea el ${raffle.drawDate}", fontSize = 12.sp, color = WarmGray)
                    }
                }
            }

            TicketRow("Costo del ticket", "${raffle.ticketCost} puntos")
            TicketRow("Tus puntos", "$balance puntos")
            TicketRow("Te quedarán", "${(balance - raffle.ticketCost).coerceAtLeast(0)} puntos")

            Surface(shape = RoundedCornerShape(12.dp), color = BadgeOrange, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Los puntos se descuentan ahora. Más tickets, más chances.",
                        fontSize = 13.sp,
                        color = InkBlack,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Comprar ticket", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            TextButton(onClick = onDismiss, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = InkBlack)
            }
        }
    }
}

@Composable
private fun TicketRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WarmGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Star, contentDescription = null, tint = RaffleGold, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
    }
}