package com.barrioahorro.app.ui.components.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.AppColors

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
    onToggleVisibility: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            placeholder = { Text(text = placeholder, color = AppColors.placeholder) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = when {
                !isPassword -> VisualTransformation.None
                isPasswordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            trailingIcon = if (isPassword && onToggleVisibility != null) {
                {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = AppColors.mutedLabel,
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = AppColors.border,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}