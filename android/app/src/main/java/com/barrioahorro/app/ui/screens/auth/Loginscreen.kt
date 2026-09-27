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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.barrioahorro.app.ui.theme.AppColors
import com.barrioahorro.app.ui.theme.Rust

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (UserType) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedNavigationEffects(viewModel, onLoginSuccess)

    LoginContent(
        state = state,
        onEmailChanged = viewModel::onLoginEmailChanged,
        onPasswordChanged = viewModel::onLoginPasswordChanged,
        onTogglePasswordVisibility = viewModel::onLoginPasswordVisibilityToggled,
        onSubmit = viewModel::submitLogin,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
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
                text = "Iniciar sesión",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = "Ingresá para ver las ofertas vigentes en tu barrio.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.mutedLabel,
                modifier = Modifier.padding(top = 8.dp),
            )

            EditorialTextField(
                label = "Email",
                value = state.email,
                onValueChange = onEmailChanged,
                placeholder = "tu@email.com",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(top = 28.dp),
            )

            EditorialTextField(
                label = "Contraseña",
                value = state.password,
                onValueChange = onPasswordChanged,
                placeholder = "••••••••",
                isPassword = true,
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                modifier = Modifier.padding(top = 18.dp),
            )

            AnimatedErrorMessage(
                message = state.errorMessage,
                modifier = Modifier.padding(top = 10.dp),
            )

            TextButton(
                onClick = { /* TODO: flujo de recuperación de contraseña */ },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = Rust,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onSubmit,
                enabled = state.isSubmitEnabled,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(text = "Iniciar sesión", modifier = Modifier.padding(vertical = 6.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "¿No tenés cuenta todavía? ",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Creá una gratis",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Rust,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister),
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

@Composable
private fun LaunchedNavigationEffects(
    viewModel: AuthViewModel,
    onLoginSuccess: (UserType) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> onLoginSuccess(event.userType)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginContent(
        state = LoginUiState(),
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onSubmit = {},
        onNavigateToRegister = {},
    )
}