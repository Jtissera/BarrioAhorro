package com.barrioahorro.app.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.cuponera.CuponeraContributionScreen
import com.barrioahorro.app.ui.onbording.BusinessCommitmentScreen
import com.barrioahorro.app.ui.screens.auth.AuthSuccessScreen
import com.barrioahorro.app.ui.screens.auth.LoginScreen
import com.barrioahorro.app.ui.screens.auth.RegisterScreen
import com.barrioahorro.app.ui.screens.auth.RoleSelectionScreen
import com.barrioahorro.app.ui.screens.auth.SessionViewModel
import com.barrioahorro.app.ui.screens.business.EditBusinessInfoScreen
import com.barrioahorro.app.ui.screens.business.EditBusinessPhotosScreen
import com.barrioahorro.app.ui.screens.business.EditBusinessPhotosViewModel
import com.barrioahorro.app.ui.screens.business.EditBusinessProfileViewModel
import com.barrioahorro.app.ui.screens.gate.GateDestination
import com.barrioahorro.app.ui.screens.gate.GateScreen
import com.barrioahorro.app.ui.screens.offers.CreateOfferScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessCategoryScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessLocationScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessNameScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessScheduleScreen
import com.barrioahorro.app.ui.screens.onboarding.OnboardingViewModel
import com.barrioahorro.app.ui.screens.splash.SplashDestination
import com.barrioahorro.app.ui.screens.splash.SplashScreen
import com.barrioahorro.app.ui.screens.splash.SplashViewModel
import com.barrioahorro.app.ui.screens.business.BusinessMainScreen
import com.barrioahorro.app.ui.cuponera.RaffleRatifyScreen
import com.barrioahorro.app.ui.cuponera.scanner.QrScannerScreen
import com.barrioahorro.app.ui.screens.admin.AdminDetailScreen
import com.barrioahorro.app.ui.screens.admin.AdminRejectScreen
import com.barrioahorro.app.ui.screens.admin.AdminListScreen
import com.barrioahorro.app.ui.screens.offers.EditOfferScreen
import com.barrioahorro.app.ui.screens.client.ClientEntryScreen
import com.barrioahorro.app.ui.screens.client.ClientMainScreen
import com.barrioahorro.app.ui.screens.client.location.ClientLocationScreen
import com.barrioahorro.app.ui.screens.client.business.ClientBusinessProfileScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoutes.SPLASH,
        modifier = modifier,
    ) {
        authGraph(navController = navController)
        onboardingGraph(navController = navController)
        businessGraph(navController = navController)
        clientGraph(navController = navController)
        adminGraph(navController = navController)
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable(SplashRoutes.SPLASH) {
        val splashViewModel: SplashViewModel = hiltViewModel()
        val destination by splashViewModel.destination.collectAsStateWithLifecycle()

        SplashScreen(
            isReady = destination != null,
            onFinished = {
                val target = when (val d = destination) {
                    is SplashDestination.Home -> d.userType.homeRoute()
                    else -> AuthRoutes.ROLE_SELECTION
                }
                navController.navigate(target) {
                    popUpTo(SplashRoutes.SPLASH) { inclusive = true }
                }
            },
        )
    }

    composable(GateRoutes.GATE) {
        GateScreen(
            onResolved = { destination ->
                val target = when (destination) {
                    GateDestination.ONBOARDING -> OnboardingRoutes.BUSINESS_NAME
                    GateDestination.CUPONERA -> CuponeraRoutes.CONTRIBUTION
                    GateDestination.HOME -> BusinessRoutes.DASHBOARD
                    GateDestination.SESSION_EXPIRED -> AuthRoutes.ROLE_SELECTION
                    GateDestination.RESTRICTED -> BusinessRoutes.DASHBOARD
                }
                navController.navigate(target) {
                    popUpTo(GateRoutes.GATE) { inclusive = true }
                }
            },
        )
    }

    composable(AuthRoutes.ROLE_SELECTION) {
        RoleSelectionScreen(
            onContinue = { userType -> navController.navigate(AuthRoutes.register(userType)) },
            onNavigateToLogin = { navController.navigate(AuthRoutes.LOGIN) },
        )
    }

    composable(AuthRoutes.LOGIN) {
        LoginScreen(
            onBack = { navController.popBackStack() },
            onNavigateToRegister = {
                navController.popBackStack(AuthRoutes.ROLE_SELECTION, inclusive = false)
            },
            onLoginSuccess = { userType -> navController.navigateAfterAuth(userType) },
        )
    }

    composable(
        route = AuthRoutes.REGISTER_PATTERN,
        arguments = listOf(navArgument("userType") { type = NavType.StringType }),
    ) { backStackEntry ->
        val userType = backStackEntry.arguments?.getString("userType")
            ?.let { UserType.valueOf(it) } ?: UserType.CLIENTE
        RegisterScreen(
            userType = userType,
            onBack = { navController.popBackStack() },
            onNavigateToLogin = { navController.navigate(AuthRoutes.LOGIN) { launchSingleTop = true } },
            onRegisterSuccess = { navController.navigateAfterAuth(it) },
        )
    }

    composable(
        route = AuthRoutes.SUCCESS_PATTERN,
        arguments = listOf(navArgument("userType") { type = NavType.StringType }),
    ) { backStackEntry ->
        val userType = backStackEntry.arguments?.getString("userType")
            ?.let { UserType.valueOf(it) } ?: UserType.CLIENTE
        val sessionViewModel: SessionViewModel = hiltViewModel()
        val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()

        AuthSuccessScreen(
            userType = userType,
            onContinue = {
                val target = if (userType == UserType.COMERCIO) BusinessRoutes.DASHBOARD else AuthRoutes.ROLE_SELECTION
                navController.navigate(target) {
                    popUpTo(AuthRoutes.SUCCESS_PATTERN) { inclusive = true }
                }
            },
            onLogout = {
                sessionViewModel.logout(onLoggedOut = { navController.navigateAfterLogout() })
            },
            isLoggingOut = isLoggingOut,
        )
    }

}

fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    navigation(startDestination = OnboardingRoutes.BUSINESS_NAME, route = OnboardingRoutes.GRAPH) {

        composable(OnboardingRoutes.BUSINESS_NAME) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BusinessNameScreen(
                initialName = state.businessName,
                initialDescription = state.descripcion,
                onContinue = { name, descripcion ->
                    viewModel.setBusinessName(name)
                    viewModel.setDescripcion(descripcion)
                    navController.navigate(OnboardingRoutes.BUSINESS_CATEGORY)
                },
            )
        }

        composable(OnboardingRoutes.BUSINESS_CATEGORY) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { viewModel.loadCategories() }

            BusinessCategoryScreen(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                isLoadingCategories = state.isLoadingCategories,
                isSubmitting = false,
                errorMessage = state.error,
                onSelectCategory = viewModel::selectCategory,
                onBack = { navController.popBackStack() },
                onFinish = { navController.navigate(OnboardingRoutes.BUSINESS_LOCATION) },
            )
        }

        composable(OnboardingRoutes.BUSINESS_LOCATION) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BusinessLocationScreen(
                direccion = state.direccion,
                localidad = state.localidad,
                pinLatitud = state.pinLatitud,
                pinLongitud = state.pinLongitud,
                isFetchingLocation = state.isFetchingLocation,
                isValidatingAddress = state.isValidatingAddress,
                isResolvingPinAddress = state.isResolvingPinAddress,
                errorMessage = state.error,
                alturaEncontrada = state.alturaEncontrada,
                mensajeValidacion = state.mensajeValidacion,

                onDireccionChange = viewModel::setDireccion,
                onLocalidadChange = viewModel::setLocalidad,
                onValidateAddress = viewModel::validateAddress,
                onRequestCurrentLocation = viewModel::fetchCurrentLocation,
                onLocationPermissionDenied = viewModel::onLocationPermissionDenied,
                onPinMoved = viewModel::onPinMoved,
                onBack = { navController.popBackStack() },
                onContinue = {
                    if (viewModel.confirmLocation()) {
                        navController.navigate(OnboardingRoutes.BUSINESS_SCHEDULE)
                    }
                },
            )
        }

        composable(OnboardingRoutes.BUSINESS_SCHEDULE) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BusinessScheduleScreen(
                schedule = state.schedule,
                onToggleDay = viewModel::toggleDay,
                onAddSlot = viewModel::addSlot,
                onRemoveSlot = viewModel::removeSlot,
                onUpdateSlotStart = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaInicio = time) },
                onUpdateSlotEnd = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaFin = time) },
                onCopyMondayToAll = viewModel::copyMondayToAll,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(OnboardingRoutes.BUSINESS_COMMITMENT) },
            )
        }

        composable(OnboardingRoutes.BUSINESS_COMMITMENT) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BusinessCommitmentScreen(
                isSubmitting = state.isSubmitting,
                errorMessage = state.error,
                onBack = { navController.popBackStack() },
                onAccept = {
                    viewModel.submitFull(
                        onSuccess = {
                            navController.navigate(OnboardingRoutes.CUPONERA) {
                                popUpTo(OnboardingRoutes.GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                },
            )
        }
    }
    composable(OnboardingRoutes.CUPONERA) {
        val activity = LocalContext.current as? Activity
        CuponeraContributionScreen(
            onBack = { activity?.finish() },
            onRatified = {
                navController.navigate(OnboardingRoutes.SORTEO) {
                    popUpTo(OnboardingRoutes.CUPONERA) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }

    composable(OnboardingRoutes.SORTEO) {
        val activity = LocalContext.current as? Activity
        RaffleRatifyScreen(
            onBack = { activity?.finish() },
            onRatified = { navController.finishRatification() },
        )
    }
}

fun NavGraphBuilder.businessGraph(navController: NavHostController) {
    composable(CuponeraRoutes.CONTRIBUTION) {
        val activity = LocalContext.current as? Activity

        CuponeraContributionScreen(
            onBack = {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    activity?.finish()
                }
            },
            onRatified = { navController.finishRatification() },
        )
    }

    composable(BusinessRoutes.DASHBOARD) { entry ->
        val sessionViewModel: SessionViewModel = hiltViewModel()
        val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()
        val requestedTab by entry.savedStateHandle
            .getStateFlow<String?>("tab", null)
            .collectAsStateWithLifecycle()

        BusinessMainScreen(
            requestedTab = requestedTab,
            onRequestedTabConsumed = { entry.savedStateHandle["tab"] = null },
            onNavigateToCreateOffer = { navController.navigate(BusinessRoutes.CREATE_OFFER) },
            onNavigateToEditOffer = { id -> navController.navigate(BusinessRoutes.editOffer(id)) },
            onNavigateToEditProfile = { navController.navigate(BusinessRoutes.EDIT_PROFILE) },
            onNavigateToEditPhotos = { navController.navigate(BusinessRoutes.EDIT_PHOTOS) },
            onNavigateToScanner = { navController.navigate(CuponeraRoutes.SCANNER) },
            onNavigateToRatifyCuponera = { navController.navigate(CuponeraRoutes.CONTRIBUTION) },
            onNavigateToRatifyRaffle = { navController.navigate(CuponeraRoutes.RAFFLE) },
            onLogout = {
                sessionViewModel.logout(onLoggedOut = { navController.navigateAfterLogout() })
            },
            isLoggingOut = isLoggingOut,
        )
    }

    composable(BusinessRoutes.CREATE_OFFER) {
        CreateOfferScreen(
            onClose = { navController.popBackStack() },
            // "Ver mis ofertas": vuelve al dashboard y selecciona la pestaña Ofertas.
            onBackToOffers = {
                navController.previousBackStackEntry?.savedStateHandle?.set("tab", "OFERTAS")
                navController.popBackStack()
            },
        )
    }

    composable(
        route = BusinessRoutes.EDIT_OFFER,
        arguments = listOf(navArgument("offerId") { type = NavType.LongType }),
    ) {
        EditOfferScreen(
            onClose = { navController.popBackStack() },
            onBackToOffers = {
                navController.previousBackStackEntry?.savedStateHandle?.set("tab", "OFERTAS")
                navController.popBackStack()
            },
        )
    }


    composable(BusinessRoutes.EDIT_PROFILE) {
        val viewModel: EditBusinessProfileViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        EditBusinessInfoScreen(
            state = state,
            onBusinessNameChange = viewModel::setBusinessName,
            onSelectCategory = viewModel::selectCategory,
            onDireccionChange = viewModel::setDireccion,
            onLocalidadChange = viewModel::setLocalidad,
            onValidateAddress = viewModel::validateAddress,
            onLocationPermissionDenied = viewModel::onLocationPermissionDenied,
            onPinMoved = viewModel::onPinMoved,
            onRequestCurrentLocation = viewModel::fetchCurrentLocation,
            onDescripcionChange = viewModel::setDescripcion,
            onToggleDay = viewModel::toggleDay,
            onAddSlot = viewModel::addSlot,
            onRemoveSlot = viewModel::removeSlot,
            onUpdateSlotStart = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaInicio = time) },
            onUpdateSlotEnd = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaFin = time) },
            onCopyMondayToAll = viewModel::copyMondayToAll,
            onRetryLoad = viewModel::loadProfile,
            onBack = { navController.popBackStack() },
            onSave = { viewModel.save(onSuccess = { navController.popBackStack() }) },
        )
    }

    composable(BusinessRoutes.EDIT_PHOTOS) {
        val viewModel: EditBusinessPhotosViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        EditBusinessPhotosScreen(
            state = state,
            onAddPhoto = viewModel::addPhoto,
            onReplacePhoto = viewModel::replacePhoto,
            onRemovePhoto = viewModel::removePhoto,
            onSetCover = viewModel::setAsCover,
            onSave = { viewModel.save(onSaved = { navController.popBackStack() }) },
            onSavedMessageShown = viewModel::onSavedMessageShown,
            onRetryLoad = viewModel::loadPhotos,
            onBack = { navController.popBackStack() },
        )

    }

    composable(CuponeraRoutes.RAFFLE) {
        RaffleRatifyScreen(
            onBack = { navController.popBackStack() },
            onRatified = { navController.finishRatification() },
        )
    }

    composable(CuponeraRoutes.SCANNER) {
        QrScannerScreen(onBack = { navController.popBackStack() })
    }


}

fun NavGraphBuilder.clientGraph(navController: NavHostController) {
    composable(ClientRoutes.ENTRY) {
        ClientEntryScreen(
            onResolved = { hasDecided ->
                val target = if (hasDecided) ClientRoutes.HOME else ClientRoutes.LOCATION
                navController.navigate(target) {
                    popUpTo(ClientRoutes.ENTRY) { inclusive = true }
                }
            },
        )
    }

    composable(
        route = ClientRoutes.LOCATION_PATTERN,
        arguments = listOf(navArgument("edit") { type = NavType.BoolType; defaultValue = false }),
    ) { entry ->
        val editing = entry.arguments?.getBoolean("edit") ?: false
        ClientLocationScreen(
            onFinished = {
                if (editing) {
                    navController.popBackStack()
                } else {
                    navController.navigate(ClientRoutes.HOME) {
                        popUpTo(ClientRoutes.LOCATION_PATTERN) { inclusive = true }
                    }
                }
            },
            onClose = { navController.popBackStack() },
        )
    }

    composable(ClientRoutes.HOME) { entry ->
        val sessionViewModel: SessionViewModel = hiltViewModel()
        val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()
        val requestedTab by entry.savedStateHandle
            .getStateFlow<String?>("tab", null)
            .collectAsStateWithLifecycle()

        ClientMainScreen(
            onEditLocation = { navController.navigate(ClientRoutes.editLocation()) },
            onLogout = { sessionViewModel.logout(onLoggedOut = { navController.navigateAfterLogout() }) },
            isLoggingOut = isLoggingOut,
            onCreatePost = { navController.navigate(ClientRoutes.CREATE_POST) },
            onPostClick = { postId -> navController.navigate(ClientRoutes.postDetail(postId)) },
            onMyPosts = { navController.navigate(ClientRoutes.MY_POSTS) },
            requestedTab = requestedTab,
            onRequestedTabConsumed = { entry.savedStateHandle["tab"] = null },
            onOpenBusiness = { id -> navController.navigate(ClientRoutes.businessProfile(id)) },
        )
    }

    composable(ClientRoutes.MY_POSTS) {
        com.barrioahorro.app.ui.screens.client.forum.MyPostsScreen(
            onNavigateBack = { navController.popBackStack() },
            onPostClick = { postId -> navController.navigate(ClientRoutes.postDetail(postId)) },
            onEditPost = { postId -> navController.navigate(ClientRoutes.editPost(postId)) },
        )
    }

    composable(
        route = ClientRoutes.BUSINESS_PROFILE,
        arguments = listOf(navArgument("businessId") { type = NavType.LongType }),
    ) {
        ClientBusinessProfileScreen(
            onBack = { navController.popBackStack() },
            // Cualquier pestaña de la barra (Explorar incluida) vuelve al home en esa pestaña.
            // El home sigue en el back stack, así que Explorar conserva lista/mapa y filtros.
            onTabSelected = { tab ->
                runCatching { navController.getBackStackEntry(ClientRoutes.HOME) }
                    .getOrNull()?.savedStateHandle?.set("tab", tab.name)
                navController.popBackStack(ClientRoutes.HOME, inclusive = false)
            },
        )
    }

    composable(ClientRoutes.CREATE_POST) {
        com.barrioahorro.app.ui.screens.client.forum.CreatePostScreen(
            onNavigateBack = { navController.popBackStack() },
            onPostCreated = { navController.popBackStack() },
        )
    }

    composable(
        route = ClientRoutes.POST_DETAIL,
        arguments = listOf(navArgument("postId") { type = NavType.LongType }),
    ) {
        com.barrioahorro.app.ui.screens.client.forum.PostDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onEditPost = { postId -> navController.navigate(ClientRoutes.editPost(postId)) },
        )
    }

    composable(
        route = ClientRoutes.EDIT_POST,
        arguments = listOf(navArgument("postId") { type = NavType.LongType }),
    ) {
        // Al volver, Mis publicaciones y el detalle recargan en ON_RESUME.
        com.barrioahorro.app.ui.screens.client.forum.EditPostScreen(
            onNavigateBack = { navController.popBackStack() },
            onPostSaved = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.adminGraph(navController: NavHostController) {
    composable(AdminRoutes.LIST) {
        val session: SessionViewModel = hiltViewModel()
        AdminListScreen(
            onOpen = { kind, id -> navController.navigate(AdminRoutes.detail(kind, id)) },
            onLogout = { session.logout(onLoggedOut = { navController.navigateAfterLogout() }) },
        )
    }

    val args = listOf(
        navArgument("kind") { type = NavType.StringType },
        navArgument("id") { type = NavType.LongType },
    )

    composable(AdminRoutes.DETAIL, arguments = args) {
        AdminDetailScreen(
            onBack = { navController.popBackStack() },
            onReject = { item -> navController.navigate(AdminRoutes.reject(item.kind, item.id)) },
            onDone = { navController.popBackStack(AdminRoutes.LIST, inclusive = false) },
        )
    }

    composable(AdminRoutes.REJECT, arguments = args) {
        AdminRejectScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack(AdminRoutes.LIST, inclusive = false) },
        )
    }
}

fun UserType.homeRoute(): String = when (this) {
    UserType.COMERCIO -> GateRoutes.GATE
    UserType.CLIENTE -> ClientRoutes.ENTRY
    UserType.ADMIN -> AdminRoutes.LIST
}
fun NavHostController.navigateAfterAuth(userType: UserType) {
    navigate(userType.homeRoute()) {
        popUpTo(AuthRoutes.ROLE_SELECTION) { inclusive = true }
    }
}

fun NavHostController.finishRatification() {
    if (previousBackStackEntry?.destination?.route == BusinessRoutes.DASHBOARD) {
        popBackStack()
    } else {
        navigate(GateRoutes.GATE) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}


fun NavHostController.navigateAfterLogout() {
    navigate(AuthRoutes.ROLE_SELECTION) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}