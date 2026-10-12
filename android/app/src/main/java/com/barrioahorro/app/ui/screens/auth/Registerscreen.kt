package com.barrioahorro.app.ui.screens.auth

import android.R.attr.shape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.R
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.components.feedback.AnimatedErrorMessage
import com.barrioahorro.app.ui.components.feedback.ErrorCard
import com.barrioahorro.app.ui.components.inputs.EditorialTextField
import com.barrioahorro.app.ui.components.inputs.PasswordStrengthIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
@Composable
fun RegisterScreen(
    userType: UserType,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (UserType) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()

    LaunchedEffect(userType) {
        viewModel.onRegisterUserTypeChanged(userType)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome ->
                    onRegisterSuccess(event.userType)
            }
        }
    }

    RegisterContent(
        state = state,
        onBack = onBack,
        onEmailChanged = viewModel::onRegisterEmailChanged,
        onPasswordChanged = viewModel::onRegisterPasswordChanged,
        onTogglePasswordVisibility =
            viewModel::onRegisterPasswordVisibilityToggled,
        onSubmit = viewModel::submitRegister,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onBack: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val background = colorResource(R.color.splash_background)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val primary = colorResource(R.color.primary)
    val onPrimary = colorResource(R.color.on_primary)
    val inactive = colorResource(R.color.neutral_inactive)

    Scaffold(
        containerColor = background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Barra superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = textPrimary,
                    )
                }

                Text(
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
            ) {
                Text(
                    text = "Registrate en minutos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Creá tu cuenta con tu Gmail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )

                EditorialTextField(
                    label = "Gmail",
                    value = state.email,
                    onValueChange = onEmailChanged,
                    placeholder = "tu@gmail.com",
                    keyboardType = KeyboardType.Email,
                    isEmailValid = isValidGmail(state.email),
                    showEmailValidation = state.email.isNotBlank(),
                    modifier = Modifier.padding(top = 24.dp),
                )

                ErrorCard(
                    title = "Este Gmail ya está registrado.".takeIf { state.emailAlreadyRegistered },
                    actionText = "¿Querés iniciar sesión ahora?",
                    onActionClick = onNavigateToLogin,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Text(
                    text = "Solo aceptamos cuentas @gmail.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
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
                        .padding(top = 10.dp),
                )

                PasswordRequirements(
                    password = state.password,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                AnimatedErrorMessage(
                    message = state.passwordTooWeakMessage,
                    modifier = Modifier.padding(top = 6.dp),
                )

                AnimatedErrorMessage(
                    message = state.errorMessage,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Button(
                    onClick = onSubmit,
                    enabled = state.isSubmitEnabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary,
                        disabledContainerColor = inactive,
                        disabledContentColor = onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = "Crear cuenta →",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "¿Ya tenés cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimary,
                    )

                    Text(
                        text = "Iniciá sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                        modifier = Modifier.clickable(
                            onClick = onNavigateToLogin,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirements(
    password: String,
    modifier: Modifier = Modifier,
) {
    val success = colorResource(R.color.secondary)
    val inactive = colorResource(R.color.neutral_inactive)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    val requirements = listOf(
        "Al menos 8 caracteres" to (password.length >= 8),
        "Una letra mayúscula" to password.any { it.isUpperCase() },
        "Al menos un número" to password.any { it.isDigit() },
        "Al menos un símbolo (ej: ! @ # $)" to
                password.any { !it.isLetterOrDigit() },
    )

    val card = colorResource(R.color.surface_card)
    val shape = RoundedCornerShape(12.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(shape)
            .background(card)
            .border(1.dp, inactive.copy(alpha = 0.3f), shape)
            .padding(14.dp),
    ) {
        requirements.forEach { (label, met) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (met) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.Circle
                    },
                    contentDescription = null,
                    tint = if (met) success else inactive,
                    modifier = Modifier.size(18.dp),
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (met) textPrimary else textSecondary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun Modifier.clip(shape: Int) {}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    RegisterContent(
        state = RegisterUiState(),
        onBack = {},
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onSubmit = {},
        onNavigateToLogin = {},
    )
}

private fun isValidGmail(email: String): Boolean {
    return email.trim().matches(
        Regex("^[A-Za-z0-9._%+-]+@gmail\\.com$", RegexOption.IGNORE_CASE)
    )
}