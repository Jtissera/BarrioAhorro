package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import kotlinx.coroutines.delay

private val WtGreen = Color(0xFF2E7D4F)
private val WtGreenSoft = Color(0xFFDDF0E3)
private val WtGraySoft = Color(0xFFE6DDD0)

@Composable
fun WinningTicketScreen(
    onBack: () -> Unit,
    viewModel: WinningTicketViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    // Mientras el QR está activo, se consulta cada 5 s para detectar el canje.
    val status = state.prize?.status
    LaunchedEffect(status) {
        while (status == PrizeStatus.CLAIMABLE) {
            delay(5_000)
            viewModel.load(silent = true)
        }
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                AwningTop()
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                    Text("Ticket ganador", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = InkBlack)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val prize = state.prize
            when {
                state.isLoading -> CircularProgressIndicator(color = Primary, modifier = Modifier.padding(top = 48.dp))

                prize == null -> Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.error ?: "No pudimos cargar el ticket.", color = WarmGray, textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Reintentar") }
                }

                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PrizeCard(prize)
                    when (prize.status) {
                        PrizeStatus.CLAIMABLE -> QrCard(prize)
                        PrizeStatus.REDEEMED -> StatusCard(
                            icon = { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                            iconBg = WtGreen,
                            title = "Premio canjeado",
                            subtitle = prize.redeemedLabel?.let { "Lo retiraste el $it." } ?: "Ya retiraste este premio.",
                        )
                        PrizeStatus.EXPIRED -> StatusCard(
                            icon = { Icon(Icons.Default.Warning, null, tint = WarmGray, modifier = Modifier.size(28.dp)) },
                            iconBg = WtGraySoft,
                            title = "Ticket vencido",
                            subtitle = "El código venció el ${prize.expiresLabel} sin ser canjeado.",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizeCard(p: PrizeUi) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(photoUrl = p.photoUrl, name = p.prizeName, size = 72.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(50), color = BadgeOrange) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, null, tint = Primary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Aporta ${p.businessName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(p.prizeName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = InkBlack)
                Text("Ticket #${p.ticketNumber}", fontSize = 12.sp, color = WarmGray)
            }
        }
    }
}

@Composable
private fun QrCard(p: PrizeUi) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu código para retirar el premio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkBlack)
            p.qrPayload?.let { QrCode(content = it, modifier = Modifier.size(240.dp)) }
            Text("Mostrale este código al comercio. Se puede usar una sola vez.", fontSize = 12.sp, color = WarmGray, textAlign = TextAlign.Center)
            Surface(shape = RoundedCornerShape(50), color = BadgeOrange) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Vence el ${p.expiresLabel}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(icon: @Composable () -> Unit, iconBg: Color, title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(56.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) { icon() }
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = InkBlack)
            Text(subtitle, fontSize = 13.sp, color = WarmGray, textAlign = TextAlign.Center)
        }
    }
}