
package com.barrioahorro.app.ui.screens.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.barrioahorro.app.ui.screens.client.coupons.CouponsTabScreen
import com.barrioahorro.app.ui.screens.client.explore.ExploreScreen
import com.barrioahorro.app.ui.screens.client.explore.FavoritesScreen
import com.barrioahorro.app.ui.screens.client.forum.ForumFeedScreen
import com.barrioahorro.app.ui.screens.client.profile.ClientProfileScreen
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.screens.client.coupons.WinnerNoticeScreen
import com.barrioahorro.app.ui.screens.client.coupons.WinnerNoticeViewModel

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
    onOpenRedemption: (Long) -> Unit = {},
    onMyRedemptions: () -> Unit = {},
    onRedemptionsHistory: () -> Unit = {},
    onMyTickets: () -> Unit = {},
    onOpenWinnerTicket: (Long) -> Unit = {},
) {

    var selectedTab by rememberSaveable {
        mutableStateOf(ClientTab.EXPLORAR)
    }

    // ViewModel de avisos de premios
    val noticeVm: WinnerNoticeViewModel = hiltViewModel()

    val notices by noticeVm.queue.collectAsState()

    // Cargar avisos al abrir la pantalla principal
    LaunchedEffect(Unit) {
        noticeVm.load()
    }

    // Navegación solicitada desde otras pantallas
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            ClientTab.entries
                .firstOrNull { it.name == requestedTab }
                ?.let { selectedTab = it }

            onRequestedTabConsumed()
        }
    }

    // Volver desde otra pestaña a Explorar
    BackHandler(
        enabled = selectedTab != ClientTab.EXPLORAR
    ) {
        selectedTab = ClientTab.EXPLORAR
    }

    // Contenedor general para superponer el aviso
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Scaffold(
            containerColor = Cream,
            bottomBar = {
                ClientBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
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

                    ClientTab.EXPLORAR -> ExploreScreen(
                        onEditLocation = onEditLocation,
                        onViewBusiness = onOpenBusiness,
                        onOpenOffer = onOpenOffer,
                    )

                    ClientTab.FAVORITOS -> FavoritesScreen(
                        onOpenOffer = onOpenOffer
                    )

                    ClientTab.FORO -> ForumFeedScreen(
                        onCreatePost = onCreatePost,
                        onPostClick = onPostClick,
                    )

                    ClientTab.CUPONES -> CouponsTabScreen(
                        modifier = Modifier.statusBarsPadding(),
                        onGoToForum = {
                            selectedTab = ClientTab.FORO
                        },
                        onOpenRedemption = onOpenRedemption,
                        onMyRedemptions = onMyRedemptions,
                        onMyTickets = onMyTickets,
                    )

                    ClientTab.PERFIL -> ClientProfileScreen(
                        onMyPosts = onMyPosts,
                        onMyReviews = onMyReviews,
                        onRedemptionsHistory = onRedemptionsHistory,
                        onMyTickets = onMyTickets,
                        onEditLocation = onEditLocation,
                        onLogout = onLogout,
                        isLoggingOut = isLoggingOut,
                    )
                }
            }
        }

        // Aviso de premio superpuesto sobre toda la aplicación
        notices.firstOrNull()?.let { notice ->

            WinnerNoticeScreen(
                notice = notice,

                onOpenTicket = {
                    noticeVm.dismiss(notice.ticketId)
                    onOpenWinnerTicket(notice.ticketId)
                },

                onClose = {
                    noticeVm.dismiss(notice.ticketId)
                }
            )
        }
    }
}
