package com.barrioahorro.app.ui.navigation

object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"

    private const val SUCCESS_BASE = "auth/success"
    const val SUCCESS_PATTERN = "$SUCCESS_BASE/{userType}"

    fun success(userType: String): String = "$SUCCESS_BASE/$userType"
}