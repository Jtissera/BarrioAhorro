package com.barrioahorro.app.ui.screens.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.R

@Composable
fun GateScreen(
    onResolved: (GateDestination) -> Unit,
    viewModel: GateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val current = state
        if (current is GateUiState.Resolved) onResolved(current.destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.splash_background)),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            is GateUiState.Failed -> {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = current.message,
                        textAlign = TextAlign.Center,
                        color = colorResource(R.color.text_primary),
                    )
                    Button(
                        onClick = viewModel::check,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.primary),
                            contentColor = colorResource(R.color.on_primary),
                        ),
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            else -> CircularProgressIndicator(color = colorResource(R.color.primary))
        }
    }
}