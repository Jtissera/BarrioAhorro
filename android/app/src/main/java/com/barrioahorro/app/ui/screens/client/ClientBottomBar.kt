package com.barrioahorro.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray

enum class ClientTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    EXPLORAR("Explorar", Icons.Outlined.Storefront, Icons.Filled.Storefront),
    FAVORITOS("Favoritos", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    FORO("Foro", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    CUPONES("Cupones", Icons.Outlined.ConfirmationNumber, Icons.Filled.ConfirmationNumber),
    PERFIL("Perfil", Icons.Outlined.PersonOutline, Icons.Filled.Person),
}

@Composable
fun ClientBottomBar(
    selectedTab: ClientTab,
    onTabSelected: (ClientTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = CardWhite, shadowElevation = 8.dp, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClientTab.entries.forEach { tab ->
                NavItem(tab, selected = tab == selectedTab, onClick = { onTabSelected(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: ClientTab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Primary else WarmGray
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) BadgeOrange else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
        )
    }
}