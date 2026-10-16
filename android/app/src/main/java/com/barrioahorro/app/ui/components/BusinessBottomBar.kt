package com.barrioahorro.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
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

enum class BusinessTab {
    INICIO,
    OFERTAS,
    CUPONERA,
    PERFIL,
}

@Composable
fun BusinessBottomBar(
    selectedTab: BusinessTab,
    onTabSelected: (BusinessTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BusinessBottomBar(
        selectedTab = selectedTab,
        onInicioClick = { onTabSelected(BusinessTab.INICIO) },
        onOfertasClick = { onTabSelected(BusinessTab.OFERTAS) },
        onCuponeraClick = { onTabSelected(BusinessTab.CUPONERA) },
        onPerfilClick = { onTabSelected(BusinessTab.PERFIL) },
        modifier = modifier,
    )
}

@Composable
fun BusinessBottomBar(
    selectedTab: BusinessTab,
    onInicioClick: () -> Unit,
    onOfertasClick: () -> Unit,
    onCuponeraClick: () -> Unit,
    onPerfilClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = CardWhite,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Inicio
            BottomNavItem(
                icon = Icons.Outlined.Home,
                label = "Inicio",
                isSelected = selectedTab == BusinessTab.INICIO,
                onClick = onInicioClick,
            )

            // Ofertas
            BottomNavItem(
                icon = Icons.Outlined.LocalOffer,
                label = "Ofertas",
                isSelected = selectedTab == BusinessTab.OFERTAS,
                onClick = onOfertasClick,
            )

            // Cuponera
            BottomNavItem(
                icon = Icons.Outlined.ConfirmationNumber,
                label = "Cuponera",
                isSelected = selectedTab == BusinessTab.CUPONERA,
                onClick = onCuponeraClick,
            )

            // Perfil
            BottomNavItem(
                icon = Icons.Outlined.Storefront,
                label = "Perfil",
                isSelected = selectedTab == BusinessTab.PERFIL,
                onClick = onPerfilClick,
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = Primary
    val textSecondaryColor = WarmGray

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) BadgeOrange else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) primaryColor else textSecondaryColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) primaryColor else textSecondaryColor,
        )
    }
}
