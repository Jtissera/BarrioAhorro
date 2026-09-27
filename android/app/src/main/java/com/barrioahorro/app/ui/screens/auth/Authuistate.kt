package com.barrioahorro.app.ui.screens.auth

import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.domain.usecase.auth.PasswordStrength

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

data class RegisterUiState(
    val userType: UserType = UserType.CLIENTE,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    private val passwordsMatch: Boolean
        get() = confirmPassword.isNotEmpty() && password == confirmPassword

    val passwordTooWeakMessage: String?
        get() = "La contraseña es muy débil".takeIf {
            password.isNotEmpty() && passwordStrength == PasswordStrength.WEAK
        }

    val passwordsDoNotMatchMessage: String?
        get() = "Las contraseñas no coinciden".takeIf {
            confirmPassword.isNotEmpty() && !passwordsMatch
        }

    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() &&
                password.length >= MIN_PASSWORD_LENGTH &&
                passwordsMatch &&
                !isLoading

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}

sealed class AuthNavigationEvent {
    data class NavigateToHome(val userType: UserType) : AuthNavigationEvent()
}