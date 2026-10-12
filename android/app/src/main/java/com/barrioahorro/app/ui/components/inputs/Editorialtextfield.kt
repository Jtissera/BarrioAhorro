package com.barrioahorro.app.ui.components.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import androidx.compose.material.icons.filled.CheckCircle

@Composable
fun EditorialTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    isEmailValid: Boolean = false,
    showEmailValidation: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
) {
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val primary = colorResource(R.color.primary)
    val inactive = colorResource(R.color.neutral_inactive)
    val white = colorResource(R.color.surface_card)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = textSecondary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isPassword) {
                        Icons.Outlined.Lock
                    } else {
                        Icons.Outlined.Email
                    },
                    contentDescription = if (isPassword) {
                        "Contraseña"
                    } else {
                        "Correo electrónico"
                    },
                    tint = textSecondary,
                )
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = when {
                !isPassword -> VisualTransformation.None
                isPasswordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            trailingIcon = when {
                isPassword && onToggleVisibility != null -> {
                    {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (isPasswordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (isPasswordVisible) {
                                    "Ocultar contraseña"
                                } else {
                                    "Mostrar contraseña"
                                },
                                tint = textSecondary,
                            )
                        }
                    }
                }

                showEmailValidation && isEmailValid -> {
                    {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Gmail válido",
                            tint = colorResource(R.color.secondary),
                        )
                    }
                }

                else -> null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = white,
                unfocusedContainerColor = white,
                disabledContainerColor = white,
                focusedBorderColor = primary,
                unfocusedBorderColor = inactive,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = primary,
                focusedLeadingIconColor = textSecondary,
                unfocusedLeadingIconColor = textSecondary,
            ),
        )
    }
}
