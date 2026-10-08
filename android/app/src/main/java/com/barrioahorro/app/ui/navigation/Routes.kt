package com.barrioahorro.app.ui.navigation

object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"

    private const val SUCCESS_BASE = "auth/success"
    const val SUCCESS_PATTERN = "$SUCCESS_BASE/{userType}"

    fun success(userType: String): String = "$SUCCESS_BASE/$userType"
}

object OnboardingRoutes {
    const val GRAPH = "onboarding_graph"
    const val BUSINESS_NAME = "onboarding/business-name"
    const val BUSINESS_CATEGORY = "onboarding/business-category"
    const val BUSINESS_LOCATION = "onboarding/business-location"
    const val BUSINESS_SCHEDULE = "onboarding/business-schedule"
}

object BusinessRoutes {
    const val EDIT_PROFILE = "business/edit-profile"
}
