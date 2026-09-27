package com.barrioahorro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.barrioahorro.app.ui.screens.auth.AuthSuccessScreen
import com.barrioahorro.app.ui.screens.auth.LoginScreen
import com.barrioahorro.app.ui.screens.auth.RegisterScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessCategoryScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessLocationScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessNameScreen
import com.barrioahorro.app.ui.screens.onboarding.BusinessScheduleScreen
import com.barrioahorro.app.ui.screens.onboarding.OnboardingViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN,
        modifier = modifier,
    ) {
        authGraph(navController = navController)
        onboardingGraph(navController = navController)
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable(AuthRoutes.LOGIN) {
        LoginScreen(
            onNavigateToRegister = { navController.navigate(AuthRoutes.REGISTER) },
            onLoginSuccess = { userType -> navController.navigateAfterAuth(userType) },
        )
    }
    composable(AuthRoutes.REGISTER) {
        RegisterScreen(
            onNavigateToLogin = { navController.popBackStack() },
            onRegisterSuccess = { userType -> navController.navigateAfterAuth(userType) },
        )
    }
    composable(
        route = AuthRoutes.SUCCESS_PATTERN,
        arguments = listOf(navArgument("userType") { type = NavType.StringType }),
    ) { backStackEntry ->
        val userType = backStackEntry.arguments?.getString("userType")
            ?.let { UserType.valueOf(it) } ?: UserType.CLIENTE
        AuthSuccessScreen(userType = userType)
    }
}

fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    navigation(startDestination = OnboardingRoutes.BUSINESS_NAME, route = OnboardingRoutes.GRAPH) {

        composable(OnboardingRoutes.BUSINESS_NAME) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            BusinessNameScreen(
                onContinue = { businessName ->
                    viewModel.setBusinessName(businessName)
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
                latitud = state.latitud,
                longitud = state.longitud,
                isFetchingLocation = state.isFetchingLocation,
                errorMessage = state.error,
                onDireccionChange = viewModel::setDireccion,
                onRequestCurrentLocation = viewModel::fetchCurrentLocation,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnboardingRoutes.BUSINESS_SCHEDULE) },
            )
        }

        composable(OnboardingRoutes.BUSINESS_SCHEDULE) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel(
                remember(backStackEntry) { navController.getBackStackEntry(OnboardingRoutes.GRAPH) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BusinessScheduleScreen(
                descripcion = state.descripcion,
                schedule = state.schedule,
                isSubmitting = state.isSubmitting,
                errorMessage = state.error,
                onDescripcionChange = viewModel::setDescripcion,
                onToggleDay = viewModel::toggleDay,
                onAddSlot = viewModel::addSlot,
                onRemoveSlot = viewModel::removeSlot,
                onUpdateSlotStart = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaInicio = time) },
                onUpdateSlotEnd = { dia, slotId, time -> viewModel.updateSlotTime(dia, slotId, horaFin = time) },
                onCopyMondayToAll = viewModel::copyMondayToAll,
                onBack = { navController.popBackStack() },
                onFinish = {
                    viewModel.submitFull(
                        onSuccess = {
                            navController.navigate(AuthRoutes.success(UserType.COMERCIO.name)) {
                                popUpTo(OnboardingRoutes.GRAPH) { inclusive = true }
                            }
                        },
                    )
                },
            )
        }
    }
}

private fun NavHostController.navigateAfterAuth(userType: UserType) {
    if (userType == UserType.COMERCIO) {
        navigate(OnboardingRoutes.BUSINESS_NAME) {
            popUpTo(AuthRoutes.LOGIN) { inclusive = true }
        }
    } else {
        navigate(AuthRoutes.success(userType.name)) {
            popUpTo(AuthRoutes.LOGIN) { inclusive = true }
        }
    }
}