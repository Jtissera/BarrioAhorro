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
    const val CUPONERA = "onboarding/cuponera"
    const val SORTEO = "onboarding/sorteo"
}

object BusinessRoutes {
    const val DASHBOARD = "business/dashboard"
    const val EDIT_PROFILE = "business/edit-profile"
    const val CREATE_OFFER = "business/create-offer"
    const val EDIT_PHOTOS = "business/edit-photos"
    const val EDIT_OFFER = "business/edit-offer/{offerId}"

    fun editOffer(id: Long): String = "business/edit-offer/$id"
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

object ClientRoutes {
    const val ENTRY = "client/entry"
    const val LOCATION = "client/location"
    const val LOCATION_PATTERN = "client/location?edit={edit}"
    const val HOME = "client/home"
    const val CREATE_POST = "client/forum/create"
    const val POST_DETAIL = "client/forum/post/{postId}"
    const val MY_POSTS = "client/forum/mine"
    const val EDIT_POST = "client/forum/post/{postId}/edit"

    fun editLocation(): String = "client/location?edit=true"
    fun postDetail(postId: Long): String = "client/forum/post/$postId"
    fun editPost(postId: Long): String = "client/forum/post/$postId/edit"
}

object AdminRoutes {
    const val LIST = "admin/list"
    const val DETAIL = "admin/detail/{kind}/{id}"
    const val REJECT = "admin/reject/{kind}/{id}"

    fun detail(kind: String, id: Long) = "admin/detail/$kind/$id"
    fun reject(kind: String, id: Long) = "admin/reject/$kind/$id"
}