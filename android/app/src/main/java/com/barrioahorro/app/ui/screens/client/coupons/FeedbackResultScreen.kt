package com.barrioahorro.app.ui.screens.client.coupons

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.barrioahorro.app.ui.components.ConfettiOverlay
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

/** outcome: "THANKS" (confirmó la recepción) | "REPORTED" (mandó un reporte). */
@Composable
fun FeedbackResultScreen(
    outcome: String,
    onPrimary: () -> Unit,
    viewModel: FeedbackResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val thanks = outcome == "THANKS"

    BackHandler(onBack = onPrimary)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Cream,
            topBar = {
                Column {
                    AwningTop()
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onPrimary) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BigBadge(Icons.Default.Check, container = FeedbackGreenSoft, content = FeedbackGreenDark)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(50), color = FeedbackGreenSoft) {
                    Text(
                        if (thanks) "Operación registrada" else "Recibido",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FeedbackGreenDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (thanks) "¡Gracias por confirmar!" else "Reporte enviado",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = InkBlack,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (thanks) "Tu confirmación ayuda a mantener la transparencia entre vecinos y comercios."
                    else "Lo vamos a revisar.",
                    color = WarmGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))

                val redemption = state.redemption
                if (redemption != null) {
                    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                        if (thanks) {
                            ThanksCard(redemption, state.pointsAwarded)
                        } else {
                            ReportedCard(redemption)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onPrimary,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (thanks) "Volver a Cupones" else "Volver a mis canjes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        ConfettiOverlay(Modifier.fillMaxSize())
    }
}

/** 4 tramos: canjeado, validado, recibido y puntos sumados. */
@Composable
private fun StepBar(filled: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(if (i < filled) Primary else Color(0xFFEADFCF), RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun ThanksCard(r: RedemptionUi, pointsAwarded: Int) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepBar(filled = if (pointsAwarded > 0) 4 else 3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(photoUrl = r.photoUrl, name = r.productName, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
                Text("Ticket ${r.receiptCode}", fontSize = 11.sp, color = WarmGray)
            }
            Surface(shape = RoundedCornerShape(50), color = FeedbackGreenSoft) {
                Text(
                    "Canjeado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FeedbackGreenDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Surface(shape = RoundedCornerShape(12.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DataRow("Beneficio", r.productName)
                DataRow("Puntos descontados", "${r.pointsCost} pts")
                if (pointsAwarded > 0) DataRow("Puntos sumados", "+$pointsAwarded pts")
                DataRow("Canjeado el", r.redeemedAt)
                r.validatedAt?.let { DataRow("Fecha y hora", it) }
            }
        }
        if (pointsAwarded > 0) {
            Surface(shape = RoundedCornerShape(12.dp), color = FeedbackGreenSoft, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = FeedbackGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sumaste +$pointsAwarded pts para tus próximos cupones",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FeedbackGreenDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportedCard(r: RedemptionUi) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        ProductThumb(photoUrl = r.photoUrl, name = r.productName, size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("TU COMERCIO LOCAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text(r.businessName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkBlack)
            Text("Pedido ${r.receiptCode} · ${r.productName}", fontSize = 11.sp, color = WarmGray)
            Text("Respuesta estimada: menos de 24 hs", fontSize = 11.sp, color = WarmGray)
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, fontSize = 12.sp, color = WarmGray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkBlack, textAlign = TextAlign.End)
    }
}