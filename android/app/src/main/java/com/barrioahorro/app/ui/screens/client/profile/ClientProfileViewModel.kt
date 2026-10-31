package com.barrioahorro.app.ui.screens.client.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import com.barrioahorro.app.data.profile.ClientProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val points: Int = 0,
    val postsCount: Int = 0,
    val reviewsCount: Int = 0,
    /** null = el vecino no configuró ninguna ubicación. */
    val locationLabel: String? = null,
)

@HiltViewModel
class ClientProfileViewModel @Inject constructor(
    private val repository: ClientProfileRepository,
    private val locationStore: ClientLocationDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ClientProfileUiState())
    val state: StateFlow<ClientProfileUiState> = _state.asStateFlow()

    private var loaded = false
    private var job: Job? = null

    init {
        // La etiqueta se actualiza sola al volver de "Mi ubicación".
        viewModelScope.launch {
            locationStore.location.collect { location ->
                _state.update { it.copy(locationLabel = location?.label) }
            }
        }
    }

    /** Se llama al entrar a la pestaña y al volver de otra pantalla. Con datos ya cargados recarga sin spinner. */
    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            if (!loaded) _state.update { it.copy(isLoading = true, error = null) }
            repository.getProfile()
                .onSuccess { profile ->
                    loaded = true
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            name = profile.name,
                            points = profile.points,
                            postsCount = profile.postsCount,
                            reviewsCount = profile.reviewsCount,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (loaded) null else (e.message ?: "No pudimos cargar tu perfil"),
                        )
                    }
                }
        }
    }
}