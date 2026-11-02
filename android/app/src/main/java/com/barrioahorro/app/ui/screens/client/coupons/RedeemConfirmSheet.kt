package com.barrioahorro.app.ui.screens.client.coupons

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemConfirmSheet(
    coupon: CouponUi,
    points: Int?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardWhite) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Confirmar canje", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = InkBlack, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, enabled = !isLoading) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Surface(shape = RoundedCornerShape(14.dp), color = Cream, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(photoUrl = coupon.photoUrl, name = coupon.productName, size = 60.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(coupon.businessName, fontSize = 12.sp, color = WarmGray)
                        Text(coupon.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkBlack)
                    }
                }
            }

            PointsRow("Costo", "${coupon.pointsCost} puntos")
            PointsRow("Tus puntos", if (points != null) "$points puntos" else "—")
            if (points != null && points >= coupon.pointsCost) {
                PointsRow("Te quedarán", "${points - coupon.pointsCost} puntos")
            }

            Surface(shape = RoundedCornerShape(12.dp), color = BadgeOrange, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Los puntos se descuentan cuando el comercio valida tu código.",
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
                    Text("Canjear", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            TextButton(onClick = onDismiss, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = InkBlack)
            }
        }
    }
}

@Composable
private fun PointsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WarmGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack)
    }
}