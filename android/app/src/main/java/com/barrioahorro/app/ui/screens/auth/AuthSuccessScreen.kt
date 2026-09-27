package com.barrioahorro.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.theme.AppColors
import com.barrioahorro.app.ui.theme.Rust

/** Placeholder mínimo hasta que exista el home real de la app. */
@Composable
fun AuthSuccessScreen(userType: UserType) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row {
                Text(text = "barrio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "ahorro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Rust)
            }

            Text(
                text = "¡Ya entraste!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = userTypeMessage(userType),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.mutedLabel,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun userTypeMessage(userType: UserType): String = when (userType) {
    UserType.CLIENTE -> "Iniciaste sesión como cliente."
    UserType.COMERCIO -> "Iniciaste sesión como comerciante."
}

@Preview(showBackground = true)
@Composable
private fun AuthSuccessScreenPreview() {
    AuthSuccessScreen(userType = UserType.CLIENTE)
}