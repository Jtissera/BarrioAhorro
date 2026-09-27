package com.barrioahorro.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val confirmPassword: String,
    val tipoUsuario: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val tipoUsuario: String,
)