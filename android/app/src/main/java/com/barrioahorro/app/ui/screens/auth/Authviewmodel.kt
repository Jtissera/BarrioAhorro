package com.barrioahorro.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.domain.model.AuthError
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.auth.EvaluatePasswordStrengthUseCase
import com.barrioahorro.app.domain.usecase.auth.LoginUseCase
import com.barrioahorro.app.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val evaluatePasswordStrength: EvaluatePasswordStrengthUseCase,
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _navigationEvents = Channel<AuthNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // ---- Login ----------------------------------------------------------

    fun onLoginEmailChanged(email: String) {
        _loginState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _loginState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onLoginPasswordVisibilityToggled() {
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitLogin() {
        val state = _loginState.value
        if (!state.isSubmitEnabled) return

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = loginUseCase(state.email, state.password)) {
                is Result.Success -> {
                    _loginState.update { it.copy(isLoading = false) }
                    _navigationEvents.send(AuthNavigationEvent.NavigateToHome(result.value.userType))
                }
                is Result.Failure -> _loginState.update {
                    it.copy(isLoading = false, errorMessage = result.error.toMessage())
                }
            }
        }
    }

    // ---- Registro ---------------------------------------------------------

    fun onRegisterUserTypeChanged(userType: UserType) {
        _registerState.update { it.copy(userType = userType, errorMessage = null) }
    }

    fun onRegisterEmailChanged(email: String) {
        _registerState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onRegisterPasswordChanged(password: String) {
        _registerState.update {
            it.copy(
                password = password,
                passwordStrength = evaluatePasswordStrength(password),
                errorMessage = null,
            )
        }
    }

    fun onRegisterConfirmPasswordChanged(confirmPassword: String) {
        _registerState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun onRegisterPasswordVisibilityToggled() {
        _registerState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRegisterConfirmPasswordVisibilityToggled() {
        _registerState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun submitRegister() {
        val state = _registerState.value
        if (!state.isSubmitEnabled) return

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = registerUseCase(
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                userType = state.userType,
            )

            when (result) {
                is Result.Success -> {
                    _registerState.update { it.copy(isLoading = false) }
                    _navigationEvents.send(AuthNavigationEvent.NavigateToHome(result.value.userType))
                }
                is Result.Failure -> _registerState.update {
                    it.copy(isLoading = false, errorMessage = result.error.toMessage())
                }
            }
        }
    }

    private fun AuthError.toMessage(): String = when (this) {
        AuthError.InvalidCredentials -> "Email o contraseña incorrectos"
        AuthError.EmailAlreadyExists -> "Ya existe una cuenta con ese email"
        AuthError.PasswordMismatch -> "Las contraseñas no coinciden"
        AuthError.NoConnection -> "No pudimos conectarnos. Revisá tu internet e intentá de nuevo"
        is AuthError.Unknown -> message ?: "Algo salió mal. Intentá de nuevo"
    }
}