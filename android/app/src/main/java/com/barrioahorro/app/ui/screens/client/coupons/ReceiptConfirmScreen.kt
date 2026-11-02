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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.components.ConfettiOverlay
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

@Composable
fun ReceiptConfirmScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    onNotReceived: () -> Unit,
    viewModel: ReceiptConfirmViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.answered) { if (state.answered) onBack() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Cream,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { FeedbackTopBar("Detalle de cupón", onBack) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                val redemption = state.redemption
                when {
                    state.isLoading && redemption == null -> CircularProgressIndicator(
                        color = Primary,
                        modifier = Modifier.padding(top = 48.dp),
                    )

                    redemption == null -> Column(
                        Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(state.error ?: "No pudimos cargar el canje.", color = WarmGray, textAlign = TextAlign.Center)
                        Button(onClick = viewModel::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Reintentar")
                        }
                    }

                    else -> ReceiptContent(
                        redemption = redemption,
                        isConfirming = state.isConfirming,
                        onYes = { viewModel.confirm(onConfirmed) },
                        onNo = onNotReceived,
                    )
                }
            }
        }

        // Confeti al verificarse el canje (una sola pasada, no bloquea toques).
        if (state.redemption != null) ConfettiOverlay(Modifier.fillMaxSize())
    }
}

@Composable
private fun ReceiptContent(
    redemption: RedemptionUi,
    isConfirming: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        BigBadge(Icons.Default.ShoppingCart, container = Color(0xFFFAD9CF), content = Primary)
        Spacer(Modifier.height(16.dp))
        Text("¿Recibiste tu producto?", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = InkBlack, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Contanos si el comercio te entregó tu canje.",
            color = WarmGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Cream).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("COMERCIO ADHERIDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                        Text(redemption.businessName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkBlack)
                    }
                    Surface(shape = RoundedCornerShape(50), color = Color.White) {
                        Text(
                            redemption.receiptCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(photoUrl = redemption.photoUrl, name = redemption.productName, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("CANJE DE PUNTOS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Text(redemption.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkBlack)
                    }
                }
                Column(
                    Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoLine(Icons.Default.DateRange, "Canjeado el ${redemption.redeemedAt}")
                    redemption.validatedAt?.let { InfoLine(Icons.Default.CheckCircle, "Validado el $it") }
                    Surface(shape = RoundedCornerShape(50), color = FeedbackGreenSoft) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = FeedbackGreenDark, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Se descontaron ${redemption.pointsCost} puntos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FeedbackGreenDark,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Confirmar la entrega ayuda a que los comercios locales reciban la liquidación de sus puntos en tiempo y forma.",
                fontSize = 12.sp,
                color = WarmGray,
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onYes,
            enabled = !isConfirming,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (isConfirming) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sí, lo recibí", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onNo,
            enabled = !isConfirming,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = BadgeOrange, contentColor = Primary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("No lo recibí", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = InkBlack)
    }
}