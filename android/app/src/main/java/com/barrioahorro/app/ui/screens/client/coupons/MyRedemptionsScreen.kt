package com.barrioahorro.app.ui.screens.client.coupons

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

private val GoldPill = Color(0xFFFBE7B0)
private val GoldStar = Color(0xFFB8860B)
private val UsedGreen = Color(0xFF2E6B4F)

/**
 * historyOnly = true  -> "Historial de canjes" (Perfil): solo canjes usados.
 * historyOnly = false -> "Mis canjes" (Cupones): todos los estados.
 */
@Composable
fun MyRedemptionsScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    historyOnly: Boolean = false,
    viewModel: MyRedemptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    val items = remember(state.items, historyOnly) {
        if (historyOnly) state.items.filter { it.status == RedemptionStatus.VALIDATED } else state.items
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            RedemptionsTopBar(
                title = if (historyOnly) "Historial de canjes" else "Mis canjes",
                points = state.points,
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.isLoading -> CircularProgressIndicator(color = Primary, modifier = Modifier.padding(top = 48.dp))

                state.error != null -> Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(state.error.orEmpty(), color = WarmGray, textAlign = TextAlign.Center)
                    Button(onClick = viewModel::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Reintentar")
                    }
                }

                items.isEmpty() -> Text(
                    if (historyOnly) "Todavía no usaste ningún cupón." else "Todavía no canjeaste ningún cupón.",
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 48.dp, start = 32.dp, end = 32.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (historyOnly && state.spentThisMonth > 0) {
                        item { SavedBanner(points = state.spentThisMonth, month = state.monthName) }
                    }
                    items(items, key = { it.id }) { r ->
                        RedemptionRowCard(
                            r = r,
                            receiptAnswered = r.id in state.answeredIds,
                            onClick = { onOpen(r.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionsTopBar(title: String, points: Int?, onBack: () -> Unit) {
    Column {
        AwningTop()
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = InkBlack, modifier = Modifier.weight(1f))
            if (points != null) {
                Surface(shape = RoundedCornerShape(50), color = GoldPill) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$points pts disp.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedBanner(points: Int, month: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(FeedbackGreenSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = null, tint = FeedbackGreenDark, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("¡Ahorraste $points puntos en $month!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
        }
    }
}

@Composable
private fun RedemptionRowCard(r: RedemptionUi, receiptAnswered: Boolean, onClick: () -> Unit) {
    val validated = r.status == RedemptionStatus.VALIDATED

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(BadgeOrange, CircleShape), contentAlignment = Alignment.Center) {
                        Text(
                            r.businessName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        r.businessName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusPill(r.status)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(photoUrl = r.photoUrl, name = r.productName, size = 68.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            r.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = InkBlack,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = GoldPill) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        if (validated) "-${r.pointsCost} pts" else "${r.pointsCost} pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkBlack,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = WarmGray, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(r.redeemedAt, fontSize = 12.sp, color = WarmGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (validated) {
                DashedDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (receiptAnswered) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = UsedGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Recepción confirmada", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UsedGreen)
                    } else {
                        Text("¿Recibiste tu producto? ›", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: RedemptionStatus) {
    if (status == RedemptionStatus.VALIDATED) {
        Surface(shape = RoundedCornerShape(50), color = UsedGreen) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Usado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    } else {
        StatusChip(status)
    }
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = Color(0xFFF1C9C0),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
    }
}