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
    val isConfirmPasswordVisible: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmailValid: Boolean
        get() = GMAIL_REGEX.matches(email.trim())

    val passwordTooWeakMessage: String?
        get() = "La contraseña es muy débil".takeIf {
            password.isNotEmpty() && passwordStrength == PasswordStrength.WEAK
        }

    val isSubmitEnabled: Boolean
        get() = isEmailValid &&
                password.length >= MIN_PASSWORD_LENGTH &&
                passwordStrength != PasswordStrength.WEAK &&
                !isLoading

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val GMAIL_REGEX =
            Regex("^[A-Za-z0-9._%+-]+@gmail\\.com$", RegexOption.IGNORE_CASE)
    }
}

sealed class AuthNavigationEvent {
    data class NavigateToHome(val userType: UserType) : AuthNavigationEvent()
}