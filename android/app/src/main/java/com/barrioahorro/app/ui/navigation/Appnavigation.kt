package com.barrioahorro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.screens.auth.AuthSuccessScreen
import com.barrioahorro.app.ui.screens.auth.LoginScreen
import com.barrioahorro.app.ui.screens.auth.RegisterScreen

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
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable(AuthRoutes.LOGIN) {
        LoginScreen(
            onNavigateToRegister = { navController.navigate(AuthRoutes.REGISTER) },
            onLoginSuccess = { userType -> navController.navigateToSuccess(userType) },
        )
    }
    composable(AuthRoutes.REGISTER) {
        RegisterScreen(
            onNavigateToLogin = { navController.popBackStack() },
            onRegisterSuccess = { userType -> navController.navigateToSuccess(userType) },
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

private fun NavHostController.navigateToSuccess(userType: UserType) {
    navigate(AuthRoutes.success(userType.name)) {
        // No queremos volver a login/registro con el botón atrás.
        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
    }
}