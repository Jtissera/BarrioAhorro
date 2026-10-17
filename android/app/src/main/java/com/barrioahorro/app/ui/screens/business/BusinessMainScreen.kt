package com.barrioahorro.app.ui.screens.business

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.barrioahorro.app.ui.components.BusinessBottomBar
import com.barrioahorro.app.ui.components.BusinessTab
import com.barrioahorro.app.ui.cuponera.CuponeraContributionScreen
import com.barrioahorro.app.ui.screens.dashboard.DashboardScreen
import com.barrioahorro.app.ui.screens.offers.MyOffersScreen
import com.barrioahorro.app.ui.theme.Cream

@Composable
fun BusinessMainScreen(
    initialTab: BusinessTab = BusinessTab.INICIO,
    onNavigateToCreateOffer: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToEditPhotos: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val context = LocalContext.current

    // Al presionar atrás en cualquier pestaña secundaria, vuelve a Inicio en vez de salir
    BackHandler(enabled = selectedTab != BusinessTab.INICIO) {
        selectedTab = BusinessTab.INICIO
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            BusinessBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when (selectedTab) {
                BusinessTab.INICIO -> DashboardScreen(
                    onAddOffer = onNavigateToCreateOffer,
                    onSeeAllOffers = { selectedTab = BusinessTab.OFERTAS },
                    onSeeAllReviews = {
                        Toast.makeText(
                            context,
                            "La pantalla de todas las reseñas todavía no está disponible",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
                BusinessTab.OFERTAS -> MyOffersScreen(
                    onCreateOffer = onNavigateToCreateOffer,
                )
                BusinessTab.CUPONERA -> CuponeraContributionScreen(
                    onBack = { selectedTab = BusinessTab.INICIO },
                    onRatified = { selectedTab = BusinessTab.INICIO },
                )
                BusinessTab.PERFIL -> BusinessProfileScreen(
                    onEditData = onNavigateToEditProfile,
                    onEditPhotos = onNavigateToEditPhotos,
                    onManageOffers = { selectedTab = BusinessTab.OFERTAS },
                    onEditOffer = {
                        Toast.makeText(context, "Próximamente: edición de ofertas", Toast.LENGTH_SHORT).show()
                    },
                    onLogout = onLogout,
                    isLoggingOut = isLoggingOut,
                )
            }
        }
    }
}
