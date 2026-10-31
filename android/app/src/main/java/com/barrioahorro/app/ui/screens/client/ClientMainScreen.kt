package com.barrioahorro.app.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.screens.client.explore.ExploreScreen
import com.barrioahorro.app.ui.screens.client.explore.FavoritesScreen
import com.barrioahorro.app.ui.screens.client.forum.ForumFeedScreen
import com.barrioahorro.app.ui.screens.client.profile.ClientProfileScreen
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.WarmGray

@Composable
fun ClientMainScreen(
    onEditLocation: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    onCreatePost: () -> Unit = {},
    onPostClick: (Long) -> Unit = {},
    onMyPosts: () -> Unit = {},
    onMyReviews: () -> Unit = {},
    onOpenBusiness: (Long) -> Unit = {},
    onOpenOffer: (Long) -> Unit = {},
    requestedTab: String? = null,
    onRequestedTabConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ClientTab.EXPLORAR) }

    // Volver desde el perfil de un comercio o el detalle de una oferta: la barra inferior pide una pestaña.
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            ClientTab.entries.firstOrNull { it.name == requestedTab }?.let { selectedTab = it }
            onRequestedTabConsumed()
        }
    }

    // Atrás desde otra pestaña vuelve a Explorar en lugar de salir.
    BackHandler(enabled = selectedTab != ClientTab.EXPLORAR) { selectedTab = ClientTab.EXPLORAR }

    Scaffold(
        containerColor = Cream,
        bottomBar = { ClientBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when (selectedTab) {
                ClientTab.EXPLORAR -> ExploreScreen(
                    onEditLocation = onEditLocation,
                    onViewBusiness = onOpenBusiness,
                    onOpenOffer = onOpenOffer,
                )

                ClientTab.FAVORITOS -> FavoritesScreen(onOpenOffer = onOpenOffer)

                ClientTab.FORO -> ForumFeedScreen(
                    onCreatePost = onCreatePost,
                    onPostClick = onPostClick,
                )

                ClientTab.CUPONES -> ComingSoon("Cupones")

                ClientTab.PERFIL -> ClientProfileScreen(
                    onMyPosts = onMyPosts,
                    onMyReviews = onMyReviews,
                    onEditLocation = onEditLocation,
                    onLogout = onLogout,
                    isLoggingOut = isLoggingOut,
                )
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Cream).statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = InkBlack)
        Text(
            text = "Esta sección estará disponible muy pronto.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}