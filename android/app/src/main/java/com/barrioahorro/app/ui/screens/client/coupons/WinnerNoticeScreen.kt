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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.data.raffles.WinnerNoticeDto
import com.barrioahorro.app.ui.components.ConfettiOverlay
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

private val NoticeGold = Color(0xFFB8860B)
private val NoticeGoldSoft = Color(0xFFFBE7B0)
private val NoticeGlow = Color(0xFFFFE3D3)

@Composable
fun WinnerNoticeScreen(
    notice: WinnerNoticeDto,
    onOpenTicket: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(Cream).statusBarsPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(130.dp).background(NoticeGlow, CircleShape))
                Box(Modifier.size(84.dp).background(NoticeGold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Text("¡Ganaste un sorteo!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = InkBlack, textAlign = TextAlign.Center)
            Text("Tu ticket fue el elegido, vecino.", fontSize = 14.sp, color = WarmGray, textAlign = TextAlign.Center)

            Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrizePhoto(
                        photoUrl = notice.prizePhotoUrl,
                        name = notice.prizeName,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                    Surface(shape = RoundedCornerShape(50), color = BadgeOrange) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(notice.raffleTitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                        }
                    }
                    Column {
                        Text("PREMIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                        Text(notice.prizeName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = InkBlack)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFAF1DF), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Mostrá el código QR de tu ticket en el comercio para retirar tu premio.",
                            fontSize = 12.sp, color = WarmGray, modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            Button(
                onClick = onOpenTicket,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Ver mi ticket →", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            TextButton(onClick = onClose) { Text("Cerrar", color = WarmGray) }
        }
        ConfettiOverlay(Modifier.fillMaxSize())
    }
}