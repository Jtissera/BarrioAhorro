package com.barrioahorro.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import com.barrioahorro.app.ui.theme.Cream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ClientEntryViewModel @Inject constructor(
    private val locationStore: ClientLocationDataStore,
) : ViewModel() {

    suspend fun hasDecidedLocation(): Boolean = locationStore.decided.first()
}

/** Pantalla de paso (sin contenido): decide si el vecino debe pasar por el onboarding de ubicación. */
@Composable
fun ClientEntryScreen(
    onResolved: (hasDecided: Boolean) -> Unit,
    viewModel: ClientEntryViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { onResolved(viewModel.hasDecidedLocation()) }
    Box(Modifier.fillMaxSize().background(Cream))
}