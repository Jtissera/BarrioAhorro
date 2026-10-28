package com.barrioahorro.app.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ClientMainScreen(
    onEditLocation: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    onCreatePost: () -> Unit = {},
    onPostClick: (Long) -> Unit = {},
    onMyPosts: () -> Unit = {},
    requestedTab: String? = null,
    onRequestedTabConsumed: () -> Unit = {},
    onOpenBusiness: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ClientTab.EXPLORAR) }

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
                ClientTab.EXPLORAR -> ExploreScreen(onEditLocation = onEditLocation, onViewBusiness = onOpenBusiness)
                ClientTab.FAVORITOS -> FavoritesScreen()
                ClientTab.FORO -> ForumFeedScreen(
                    onCreatePost = onCreatePost,
                    onPostClick = onPostClick,
                )
                ClientTab.CUPONES -> ComingSoon("Cupones")
                ClientTab.PERFIL -> ClientProfileTab(
                    onMyPosts = onMyPosts,
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

/** Provisorio: la pantalla definitiva de perfil del vecino reutiliza este mismo onLogout. */
@Composable
private fun ClientProfileTab(
    onMyPosts: () -> Unit,
    onEditLocation: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Cream).statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = InkBlack)

        Button(
            onClick = onMyPosts,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text("Mis publicaciones", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onEditLocation,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp),
        ) {
            Text("Cambiar ubicación", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onLogout,
            enabled = !isLoggingOut,
            shape = RoundedCornerShape(27.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp),
        ) {
            Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
        }
    }
}