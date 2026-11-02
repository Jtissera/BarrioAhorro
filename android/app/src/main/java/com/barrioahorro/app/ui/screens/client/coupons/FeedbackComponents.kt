package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack

internal val FeedbackGreenSoft = Color(0xFFDDF0E3)
internal val FeedbackGreenDark = Color(0xFF2E7D4F)

@Composable
internal fun AwningTop() {
    ToldoConFranjas(
        altColor = Cream,
        modifier = Modifier.background(Cream).statusBarsPadding(),
    )
}

@Composable
internal fun FeedbackTopBar(title: String, onBack: () -> Unit) {
    Column {
        AwningTop()
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = InkBlack)
        }
    }
}

@Composable
internal fun BigBadge(icon: ImageVector, container: Color, content: Color) {
    Box(Modifier.size(112.dp).background(container.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(88.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(44.dp))
        }
    }
}