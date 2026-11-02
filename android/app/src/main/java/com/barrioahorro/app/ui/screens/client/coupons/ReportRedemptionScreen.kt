package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportRedemptionScreen(
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: ReportRedemptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val blank = state.description.isBlank()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { FeedbackTopBar("Detalle de cupón", onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.redemption?.let { r ->
                Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductThumb(photoUrl = r.photoUrl, name = r.productName, size = 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.businessName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                            Text(r.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkBlack)
                        }
                        Surface(shape = RoundedCornerShape(50), color = Cream) {
                            Text(
                                "Pedido ${r.receiptCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmGray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            Text("Motivo principal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarmGray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportReason.entries.forEach { reason ->
                    SelectableChip(
                        label = reason.label,
                        selected = state.reason == reason,
                        onClick = { viewModel.selectReason(reason) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Qué pasó? *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack, modifier = Modifier.weight(1f))
                Text(
                    if (blank && !state.showEmptyError) "Obligatorio" else "${state.description.length}/$REPORT_MAX_LENGTH",
                    fontSize = 11.sp,
                    fontWeight = if (blank) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (state.showEmptyError || (blank && !state.showEmptyError)) Primary else WarmGray,
                )
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                isError = state.showEmptyError,
                placeholder = { Text("Ej: Las medialunas no llegaron frescas o faltó un ítem…", fontSize = 13.sp) },
                minLines = 5,
                maxLines = 8,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color(0xFFFFF1EE),
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Color(0xFFEADFCF),
                    errorBorderColor = Primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.showEmptyError) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Contanos qué pasó", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }

            Text("SUGERENCIAS RÁPIDAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmGray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REPORT_SUGGESTIONS.forEach { text ->
                    SelectableChip(label = text, selected = false, onClick = { viewModel.applySuggestion(text) })
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = WarmGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tu respuesta es confidencial", fontSize = 12.sp, color = WarmGray)
            }

            // Vacío: se ve apagado (como en el mock) pero al tocarlo marca el error.
            Button(
                onClick = { viewModel.submit(onSent) },
                enabled = !state.isSending,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (blank) Color(0xFFFBEFD9) else Primary,
                    contentColor = if (blank) WarmGray else Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Enviar reporte", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            if (blank) {
                Text(
                    "Escribí qué pasó para enviar el reporte.",
                    fontSize = 12.sp,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else Color.White,
        border = BorderStroke(1.dp, if (selected) Primary else Color(0xFFEADFCF)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else InkBlack,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}