package com.barrioahorro.app.domain.model

sealed class AuthError {
    data object InvalidCredentials : AuthError()
    data object EmailAlreadyExists : AuthError()
    data object PasswordMismatch : AuthError()
    data object NoConnection : AuthError()
    data class Unknown(val message: String?) : AuthError()
}