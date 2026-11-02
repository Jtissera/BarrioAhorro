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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

@Composable
fun RedemptionDetailScreen(
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    viewModel: RedemptionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(state.promptReceipt) {
        if (state.promptReceipt) {
            val id = state.redemption?.id
            viewModel.onPromptConsumed()
            if (id != null) onOpenReceipt(id)
        }
    }

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

                else -> RedemptionCard(
                    redemption = redemption,
                    isCancelling = state.isCancelling,
                    needsReceiptAnswer = state.feedbackAnswered == false,
                    onCancel = { confirmCancel = true },
                    onOpenReceipt = { onOpenReceipt(redemption.id) },
                )
            }
        }
    }

    if (confirmCancel) {
        CancelDialog(
            points = state.points,
            onConfirm = { confirmCancel = false; viewModel.cancel() },
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
private fun CancelDialog(points: Int?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = CardWhite) {
            Column {
                ToldoConFranjas(modifier = Modifier.fillMaxWidth())
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BigBadge(Icons.Default.Refresh, container = BadgeOrange, content = Primary)
                    Spacer(Modifier.height(16.dp))
                    Text("¿Cancelar canje?", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = InkBlack)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Recuperás el cupón. No se descontaron puntos.",
                        color = WarmGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (points != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(50), color = FeedbackGreenSoft) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FeedbackGreenDark, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Tus $points puntos siguen a salvo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FeedbackGreenDark,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("Cancelar canje", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver", color = InkBlack, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionCard(
    redemption: RedemptionUi,
    isCancelling: Boolean,
    needsReceiptAnswer: Boolean,
    onCancel: () -> Unit,
    onOpenReceipt: () -> Unit,
) {
    val pending = redemption.status == RedemptionStatus.PENDING

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(50), color = BadgeOrange) {
                    Text(
                        redemption.businessName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (pending && redemption.code != null) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                        QrCode(content = redemption.code, modifier = Modifier.padding(12.dp).size(220.dp))
                    }
                } else {
                    InactiveBadge(redemption.status)
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    redemption.productName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = InkBlack,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(redemption.status)
                    Surface(shape = RoundedCornerShape(50), color = Cream) {
                        Text(
                            "Pedido ${redemption.receiptCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Surface(shape = RoundedCornerShape(14.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow(Icons.Default.DateRange, "Canjeado el ${redemption.redeemedAt}")
                        InfoRow(Icons.Default.DateRange, "Válido hasta el ${redemption.validUntil}")
                        InfoRow(Icons.Default.Star, pointsText(redemption))
                        redemption.validatedAt?.let { InfoRow(Icons.Default.CheckCircle, "Validado el $it") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (pending) {
            Text("Mostrá este código en el comercio.", color = WarmGray, fontSize = 14.sp)
            TextButton(onClick = onCancel, enabled = !isCancelling) {
                if (isCancelling) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Cancelar canje", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (redemption.status == RedemptionStatus.VALIDATED && needsReceiptAnswer) {
            Button(
                onClick = onOpenReceipt,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("¿Recibiste tu producto?", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

private fun pointsText(r: RedemptionUi): String = when (r.status) {
    RedemptionStatus.PENDING -> "${r.pointsCost} puntos, se descuentan al validar"
    RedemptionStatus.VALIDATED -> "${r.pointsCost} puntos descontados"
    else -> "No se descontaron puntos"
}

@Composable
private fun InactiveBadge(status: RedemptionStatus) {
    Box(
        Modifier.size(120.dp).background(status.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (status == RedemptionStatus.VALIDATED) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = status.content,
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WarmGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = InkBlack)
    }
}