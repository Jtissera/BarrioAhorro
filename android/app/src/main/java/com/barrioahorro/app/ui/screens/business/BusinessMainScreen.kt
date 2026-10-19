package com.barrioahorro.app.ui.screens.business

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.components.BusinessBottomBar
import com.barrioahorro.app.ui.components.BusinessTab
import com.barrioahorro.app.ui.cuponera.ContributionErrorState
import com.barrioahorro.app.ui.cuponera.MyContributionScreen
import com.barrioahorro.app.ui.cuponera.MyContributionUiState
import com.barrioahorro.app.ui.cuponera.MyContributionViewModel
import com.barrioahorro.app.ui.cuponera.RestrictedAccessScreen
import com.barrioahorro.app.ui.screens.dashboard.DashboardScreen
import com.barrioahorro.app.ui.screens.offers.MyOffersScreen
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Primary
import androidx.compose.runtime.LaunchedEffect

@Composable
fun BusinessMainScreen(
    initialTab: BusinessTab = BusinessTab.INICIO,
    onNavigateToCreateOffer: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToEditPhotos: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToRatifyCuponera: () -> Unit,
    onNavigateToRatifyRaffle: () -> Unit,
    onNavigateToEditOffer: (Long) -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    modifier: Modifier = Modifier,
    requestedTab: String? = null,
    onRequestedTabConsumed: () -> Unit = {},
    accessViewModel: MyContributionViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    LaunchedEffect(requestedTab) {
        val tab = requestedTab?.let { name -> BusinessTab.entries.firstOrNull { it.name == name } }
        if (tab != null) selectedTab = tab
        if (requestedTab != null) onRequestedTabConsumed()
    }

    val context = LocalContext.current

    val access by accessViewModel.uiState.collectAsStateWithLifecycle()
    // null = todavía no se sabe (cargando o con error).
    val restricted: Boolean? = access.overview?.restrictedAccess

    // Solo se puede editar con la cuenta confirmada como habilitada.
    val canEdit = restricted == false
    val readOnlyMessage = "Tu perfil está en modo lectura hasta que ratifiques tu aporte"

    // Al volver de ratificar o de escanear se actualiza el estado del acceso.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { accessViewModel.refresh() }

    // Al presionar atrás en cualquier pestaña secundaria, vuelve a Inicio en vez de salir
    BackHandler(enabled = selectedTab != BusinessTab.INICIO) {
        selectedTab = BusinessTab.INICIO
    }

    val restrictedContent: @Composable () -> Unit = {
        RestrictedAccessScreen(
            onRatifyCuponera = onNavigateToRatifyCuponera,
            onRatifyRaffle = onNavigateToRatifyRaffle,
            onViewProfile = { selectedTab = BusinessTab.PERFIL },
            viewModel = accessViewModel,
        )
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
                BusinessTab.INICIO -> AccessGate(
                    state = access,
                    onRetry = accessViewModel::load,
                    restrictedContent = restrictedContent,
                ) {
                    DashboardScreen(
                        onAddOffer = onNavigateToCreateOffer,
                        onSeeAllOffers = { selectedTab = BusinessTab.OFERTAS },
                        onSeeAllReviews = {
                            Toast.makeText(
                                context,
                                "La pantalla de todas las reseñas todavía no está disponible",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }

                BusinessTab.OFERTAS -> AccessGate(
                    state = access,
                    onRetry = accessViewModel::load,
                    restrictedContent = restrictedContent,
                ) {
                    MyOffersScreen(
                        onCreateOffer = onNavigateToCreateOffer,
                        onEditOffer = onNavigateToEditOffer,
                    )
                }

                BusinessTab.CUPONERA -> MyContributionScreen(
                    onScanQr = onNavigateToScanner,
                    onRatifyCuponera = onNavigateToRatifyCuponera,
                    onRatifyRaffle = onNavigateToRatifyRaffle,
                    viewModel = accessViewModel,
                )

                BusinessTab.PERFIL -> BusinessProfileScreen(
                    onEditData = onNavigateToEditProfile,
                    onEditPhotos = onNavigateToEditPhotos,
                    // Si está bloqueado, Ofertas muestra el aviso de acceso limitado.
                    onManageOffers = { selectedTab = BusinessTab.OFERTAS },
                    onEditOffer = { id ->
                        if (canEdit) onNavigateToEditOffer(id)
                        else Toast.makeText(context, readOnlyMessage, Toast.LENGTH_SHORT).show()
                    },
                    onLogout = onLogout,
                    isLoggingOut = isLoggingOut,
                    readOnly = !canEdit,
                )
            }
        }
    }
}

/** Muestra el contenido solo si la cuenta no está bloqueada; si lo está, la pantalla de acceso limitado. */
@Composable
private fun AccessGate(
    state: MyContributionUiState,
    onRetry: () -> Unit,
    restrictedContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val overview = state.overview
    when {
        overview?.restrictedAccess == true -> restrictedContent()
        overview != null -> content()
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        else -> ContributionErrorState(
            message = state.error ?: "No pudimos verificar el estado de tu cuenta.",
            onRetry = onRetry,
        )
    }
}