package com.barrioahorro.app.domain.model

data class AuthSession(
    val userId: Long,
    val email: String,
    val userType: UserType,
    val accessToken: String,
)