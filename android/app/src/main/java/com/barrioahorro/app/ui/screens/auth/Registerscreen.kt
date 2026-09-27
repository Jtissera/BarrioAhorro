package com.barrioahorro.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.components.feedback.AnimatedErrorMessage
import com.barrioahorro.app.ui.components.inputs.EditorialTextField
import com.barrioahorro.app.ui.components.inputs.PasswordStrengthIndicator
import com.barrioahorro.app.ui.components.inputs.UserTypeToggle
import com.barrioahorro.app.ui.theme.AppColors
import com.barrioahorro.app.ui.theme.Rust

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (UserType) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> onRegisterSuccess(event.userType)
            }
        }
    }

    RegisterContent(
        state = state,
        onUserTypeChanged = viewModel::onRegisterUserTypeChanged,
        onEmailChanged = viewModel::onRegisterEmailChanged,
        onPasswordChanged = viewModel::onRegisterPasswordChanged,
        onConfirmPasswordChanged = viewModel::onRegisterConfirmPasswordChanged,
        onTogglePasswordVisibility = viewModel::onRegisterPasswordVisibilityToggled,
        onToggleConfirmPasswordVisibility = viewModel::onRegisterConfirmPasswordVisibilityToggled,
        onSubmit = viewModel::submitRegister,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onUserTypeChanged: (UserType) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            BrandWordmark()

            Text(
                text = "Creá tu cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = "Un minuto y ya ves lo que está en oferta cerca tuyo.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.mutedLabel,
                modifier = Modifier.padding(top = 8.dp),
            )

            UserTypeToggle(
                selected = state.userType,
                onSelectedChange = onUserTypeChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            EditorialTextField(
                label = "Email",
                value = state.email,
                onValueChange = onEmailChanged,
                placeholder = "tu@email.com",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(top = 20.dp),
            )

            EditorialTextField(
                label = "Contraseña",
                value = state.password,
                onValueChange = onPasswordChanged,
                placeholder = "Mínimo 8 caracteres",
                isPassword = true,
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                modifier = Modifier.padding(top = 18.dp),
            )

            PasswordStrengthIndicator(
                strength = state.passwordStrength,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            AnimatedErrorMessage(
                message = state.passwordTooWeakMessage,
                modifier = Modifier.padding(top = 6.dp),
            )

            EditorialTextField(
                label = "Repetir contraseña",
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                placeholder = "Escribila de nuevo",
                isPassword = true,
                isPasswordVisible = state.isConfirmPasswordVisible,
                onToggleVisibility = onToggleConfirmPasswordVisibility,
                modifier = Modifier.padding(top = 18.dp),
            )

            AnimatedErrorMessage(
                message = state.passwordsDoNotMatchMessage,
                modifier = Modifier.padding(top = 6.dp),
            )

            AnimatedErrorMessage(
                message = state.errorMessage,
                modifier = Modifier.padding(top = 6.dp),
            )

            Button(
                onClick = onSubmit,
                enabled = state.isSubmitEnabled,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = AppColors.track,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(text = "Crear cuenta", modifier = Modifier.padding(vertical = 6.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "¿Ya tenés cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Iniciá sesión",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Rust,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin),
                )
            }
        }
    }
}

@Composable
private fun BrandWordmark() {
    Row {
        Text(text = "barrio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = "ahorro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Rust)
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    RegisterContent(
        state = RegisterUiState(),
        onUserTypeChanged = {},
        onEmailChanged = {},
        onPasswordChanged = {},
        onConfirmPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onToggleConfirmPasswordVisibility = {},
        onSubmit = {},
        onNavigateToLogin = {},
    )
}