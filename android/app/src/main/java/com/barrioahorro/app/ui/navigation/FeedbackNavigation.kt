package com.barrioahorro.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.barrioahorro.app.ui.screens.client.ClientTab
import com.barrioahorro.app.ui.screens.client.coupons.FeedbackResultScreen
import com.barrioahorro.app.ui.screens.client.coupons.ReceiptConfirmScreen
import com.barrioahorro.app.ui.screens.client.coupons.ReportRedemptionScreen

object FeedbackRoutes {
    const val RECEIPT = "client/redemption/{redemptionId}/receipt"
    const val REPORT = "client/redemption/{redemptionId}/report"
    const val RESULT = "client/redemption/{redemptionId}/result/{outcome}"

    fun receipt(id: Long) = "client/redemption/$id/receipt"
    fun report(id: Long) = "client/redemption/$id/report"
    fun result(id: Long, outcome: String) = "client/redemption/$id/result/$outcome"
}

fun NavGraphBuilder.feedbackGraph(navController: NavHostController) {
    val idArg = navArgument("redemptionId") { type = NavType.LongType }

    composable(FeedbackRoutes.RECEIPT, arguments = listOf(idArg)) { entry ->
        val id = entry.arguments?.getLong("redemptionId") ?: return@composable
        ReceiptConfirmScreen(
            onBack = { navController.popBackStack() },
            onConfirmed = {
                navController.navigate(FeedbackRoutes.result(id, "THANKS")) {
                    popUpTo(FeedbackRoutes.RECEIPT) { inclusive = true }
                }
            },
            onNotReceived = { navController.navigate(FeedbackRoutes.report(id)) },
        )
    }

    composable(FeedbackRoutes.REPORT, arguments = listOf(idArg)) { entry ->
        val id = entry.arguments?.getLong("redemptionId") ?: return@composable
        ReportRedemptionScreen(
            onBack = { navController.popBackStack() },
            onSent = {
                // Saca del stack el reporte y la pregunta de recepción.
                navController.navigate(FeedbackRoutes.result(id, "REPORTED")) {
                    popUpTo(FeedbackRoutes.RECEIPT) { inclusive = true }
                }
            },
        )
    }

    composable(
        FeedbackRoutes.RESULT,
        arguments = listOf(idArg, navArgument("outcome") { type = NavType.StringType }),
    ) { entry ->
        val outcome = entry.arguments?.getString("outcome") ?: "THANKS"
        FeedbackResultScreen(
            outcome = outcome,
            onPrimary = {
                if (outcome == "THANKS") {
                    navController.backToClientTab(ClientTab.CUPONES)
                } else {
                    navController.navigate(ClientRoutes.MY_REDEMPTIONS) {
                        popUpTo(ClientRoutes.HOME)
                    }
                }
            },
        )
    }
}