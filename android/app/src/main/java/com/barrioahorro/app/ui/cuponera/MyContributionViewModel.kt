package com.barrioahorro.app.ui.cuponera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CommitmentOverviewDto
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyContributionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val overview: CommitmentOverviewDto? = null,
)

@HiltViewModel
class MyContributionViewModel @Inject constructor(
    private val repository: CuponeraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyContributionUiState())
    val uiState: StateFlow<MyContributionUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Se llama al volver a la pantalla: refresca sin tapar con un spinner lo que ya se ve. */
    fun refresh() = load()

    fun load() {
        if (loadJob?.isActive == true) return

        _uiState.update { it.copy(loading = it.overview == null, error = null) }
        loadJob = viewModelScope.launch {
            repository.getOverview()
                .onSuccess { overview ->
                    _uiState.update { it.copy(loading = false, overview = overview) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "No se pudo cargar tu aporte.")
                    }
                }
        }
    }
}