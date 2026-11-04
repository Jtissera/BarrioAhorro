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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.layout.statusBarsPadding

private val TicketGreen = Color(0xFF2E7D4F)
private val TicketGreenSoft = Color(0xFFDDF0E3)
private val TicketGray = Color(0xFFE6DDD0)

@Composable
fun MyTicketsScreen(
    onBack: () -> Unit,
    viewModel: MyTicketsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    val shown = if (state.tab == MyTicketsTab.ACTIVOS) state.active else state.results

    Scaffold(
        containerColor = Cream,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                AwningTop()
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                    Text("Mis tickets", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = InkBlack, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                TicketTabs(state.tab, state.active.size, viewModel::setTab)
            }
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

                shown.isEmpty() -> Text(
                    if (state.tab == MyTicketsTab.ACTIVOS) "No tenés tickets en juego. Elegí un sorteo y sumá chances."
                    else "Todavía no hay sorteos con resultado.",
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 48.dp, start = 32.dp, end = 32.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(shown, key = { _, it -> it.raffleId }) { index, t ->
                        MyTicketCard(t, stripe = if (index % 2 == 0) Primary else TicketGreen)
                    }
                    item { InfoNote() }
                }
            }
        }
    }
}

@Composable
private fun TicketTabs(selected: MyTicketsTab, activeCount: Int, onSelect: (MyTicketsTab) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            MyTicketsTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tab == MyTicketsTab.ACTIVOS) "Activos" else "Resultados",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (isSelected) Primary else WarmGray,
                        )
                        if (tab == MyTicketsTab.ACTIVOS && activeCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(18.dp).background(BadgeOrange, CircleShape), contentAlignment = Alignment.Center) {
                                Text("$activeCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                    }
                    Box(Modifier.height(2.dp).fillMaxWidth().background(if (isSelected) Primary else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEADFCF)))
    }
}
@Composable
private fun MyTicketCard(t: MyTicketUi, stripe: Color) {
    val inPlay = t.status == MyTicketStatus.ACTIVE
    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            StripedEdge(stripe)
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Cream, modifier = Modifier.weight(1f, fill = false)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = WarmGray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Aporta ${t.businessName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = InkBlack,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TicketStatusPill(t.status)
                }

                Text(t.prizeName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = InkBlack, maxLines = 2, overflow = TextOverflow.Ellipsis)
                t.description?.let {
                    Text(it, fontSize = 12.sp, color = WarmGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                PerforatedLine()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(BadgeOrange, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (t.tickets == 1) "1 ticket" else "${t.tickets} tickets", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
                        Text(t.numbersLabel, fontSize = 11.sp, color = WarmGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = WarmGray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(if (inPlay) "Se sortea el" else "Sorteo del", fontSize = 11.sp, color = WarmGray)
                        }
                        Text(t.drawDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketStatusPill(status: MyTicketStatus) {
    val (label, bg, fg) = when (status) {
        MyTicketStatus.ACTIVE -> Triple("Participando", TicketGreenSoft, TicketGreen)
        MyTicketStatus.WON -> Triple("¡Ganaste!", TicketGreen, Color.White)
        MyTicketStatus.NOT_WON -> Triple("No salió", TicketGray, WarmGray)
        MyTicketStatus.REFUNDED -> Triple("Reembolsado", TicketGray, WarmGray)
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun StripedEdge(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val stripe = 14.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(if (i % 2 == 0) color else Color.White, Offset(x, 0f), Size(minOf(stripe, size.width - x), size.height))
            x += stripe
            i++
        }
    }
}

@Composable
private fun PerforatedLine() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = Color(0xFFE6DDD0),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
    }
}

@Composable
private fun InfoNote() {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(CardWhite, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Info, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Los tickets se compran con puntos. El sorteo se realiza el último día de cada mes y te avisamos el resultado.",
            fontSize = 12.sp, color = WarmGray,
        )
    }
}