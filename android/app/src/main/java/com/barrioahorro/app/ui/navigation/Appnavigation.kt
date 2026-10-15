package com.barrioahorro.app.ui.navigation

import android.app.Activity
import android.widget.Toast
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
import com.barrioahorro.app.ui.screens.dashboard.DashboardScreen
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
                    // Un comercio nunca va directo al inicio: primero pasa por la compuerta.
                    is SplashDestination.Home ->
                        if (d.userType == UserType.COMERCIO) GateRoutes.GATE
                        else AuthRoutes.success(d.userType.name)
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
        AuthSuccessScreen(
            userType = userType,
            onCreateOffer = { navController.navigate(BusinessRoutes.CREATE_OFFER) },
        )
    }

    composable(
        route = BusinessRoutes.CREATE_OFFER) {
        val context = LocalContext.current
        CreateOfferScreen(
            onClose = { navController.popBackStack() },
            onPublished = {
                Toast.makeText(context, "¡Oferta publicada!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
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
                            navController.navigate(CuponeraRoutes.CONTRIBUTION) {
                                launchSingleTop = true
                            }
                        },
                    )
                },
            )
        }
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
            onRatified = {
                navController.navigate(BusinessRoutes.DASHBOARD) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
    }

    composable(BusinessRoutes.DASHBOARD) {
        val context = LocalContext.current

        DashboardScreen(
            onAddOffer = {
                navController.navigate(BusinessRoutes.CREATE_OFFER)
            },
            onSeeAllOffers = {
                Toast.makeText(
                    context,
                    "La pantalla de todas las ofertas todavía no está disponible",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSeeAllReviews = {
                Toast.makeText(
                    context,
                    "La pantalla de todas las reseñas todavía no está disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
            onSave = viewModel::save,
            onSavedMessageShown = viewModel::onSavedMessageShown,
            onRetryLoad = viewModel::loadPhotos,
            onBack = { navController.popBackStack() },
        )
    }
}

fun NavHostController.navigateAfterAuth(userType: UserType) {
    //  limpia toda la pila de autenticación.
    val target = if (userType == UserType.COMERCIO) {
        GateRoutes.GATE
    } else {
        AuthRoutes.success(userType.name)
    }
    navigate(target) {
        popUpTo(AuthRoutes.ROLE_SELECTION) { inclusive = true }
    }
}