package com.barrioahorro.app.ui.cuponera.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.barrioahorro.app.ui.screens.client.coupons.ProductThumb
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

private val ClaimGreen = Color(0xFF2E7D4F)

/** Se muestra cuando el escáner lee un QR que empieza con [PRIZE_QR_PREFIX]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrizeClaimHost(
    code: String,
    onDismiss: () -> Unit,
    viewModel: PrizeClaimViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(code) { viewModel.start(code) }

    ModalBottomSheet(onDismissRequest = { if (!state.confirming) onDismiss() }, containerColor = CardWhite) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val r = state.result
            when {
                state.loading -> CircularProgressIndicator(color = Primary, modifier = Modifier.padding(24.dp))

                r == null -> {
                    Text(state.error ?: "No pudimos validar el código.", color = WarmGray, textAlign = TextAlign.Center)
                    TextButton(onClick = onDismiss) { Text("Cerrar", color = InkBlack) }
                }

                state.delivered -> {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(56.dp).background(ClaimGreen, CircleShape).padding(12.dp))
                    Text("Premio entregado", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = InkBlack)
                    Text(r.prizeName.orEmpty(), color = WarmGray)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Listo", fontWeight = FontWeight.Bold) }
                }

                !r.valid -> {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(56.dp).background(Primary, CircleShape).padding(12.dp))
                    Text("Código no válido", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = InkBlack)
                    Text(r.message, color = WarmGray, textAlign = TextAlign.Center)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Cerrar", fontWeight = FontWeight.Bold) }
                }

                else -> {
                    Text("Ticket ganador válido", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = ClaimGreen)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        ProductThumb(photoUrl = r.prizePhotoUrl, name = r.prizeName.orEmpty(), size = 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(r.prizeName.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InkBlack)
                            r.prizeDescription?.let { Text(it, fontSize = 12.sp, color = WarmGray) }
                            Text("Ticket #${r.ticketNumber} · ${r.winnerName.orEmpty()}", fontSize = 12.sp, color = WarmGray)
                        }
                    }
                    state.error?.let { Text(it, color = Primary, fontSize = 12.sp) }
                    Button(
                        onClick = viewModel::confirm,
                        enabled = !state.confirming,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (state.confirming) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text("Confirmar entrega", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(onClick = onDismiss, enabled = !state.confirming) { Text("Cancelar", color = InkBlack) }
                }
            }
        }
    }
}