package com.barrioahorro.app.ui.navigation

import com.barrioahorro.app.domain.model.UserType

object AuthRoutes {
    const val LOGIN = "auth/login"

    const val ROLE_SELECTION = "auth/role_selection"

    const val REGISTER_PATTERN = "auth/register/{userType}"

    fun register(userType: UserType): String =
        "auth/register/${userType.name}"

    private const val SUCCESS_BASE = "auth/success"
    const val SUCCESS_PATTERN = "$SUCCESS_BASE/{userType}"

    fun success(userType: String): String =
        "$SUCCESS_BASE/$userType"
}

object OnboardingRoutes {
    const val GRAPH = "onboarding_graph"
    const val BUSINESS_NAME = "onboarding/business-name"
    const val BUSINESS_CATEGORY = "onboarding/business-category"
    const val BUSINESS_LOCATION = "onboarding/business-location"
    const val BUSINESS_SCHEDULE = "onboarding/business-schedule"

    const val BUSINESS_COMMITMENT = "onboarding/business-commitment"
}

object BusinessRoutes {
    const val DASHBOARD = "business/dashboard"
    const val EDIT_PROFILE = "business/edit-profile"
    const val CREATE_OFFER = "business/create-offer"
    const val EDIT_PHOTOS = "business/edit-photos"
}

object SplashRoutes {
    const val SPLASH = "splash"
}

object CuponeraRoutes {
    const val CONTRIBUTION = "cuponera/contribution"
    const val RAFFLE = "cuponera/raffle"
    const val SCANNER = "cuponera/scanner"
    const val RESTRICTED = "cuponera/restricted"
}

object GateRoutes {
    const val GATE = "gate"
}