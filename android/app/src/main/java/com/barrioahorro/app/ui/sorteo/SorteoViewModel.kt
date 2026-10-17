package com.barrioahorro.app.ui.sorteo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.sorteo.SorteoCommitmentDto
import com.barrioahorro.app.data.sorteo.SorteoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MAX_PREMIO_LENGTH = 60

data class SorteoUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val commitment: SorteoCommitmentDto? = null,
    val premio: String = "",
    val showConfirm: Boolean = false,
    val submitting: Boolean = false,
    val submitError: String? = null,
) {
    val canRatify: Boolean
        get() = commitment?.puedeRatificar == true && premio.isNotBlank() && !submitting
}

sealed interface SorteoEvent {
    data object Ratified : SorteoEvent
}

@HiltViewModel
class SorteoViewModel @Inject constructor(
    private val repository: SorteoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SorteoUiState())
    val uiState: StateFlow<SorteoUiState> = _uiState.asStateFlow()

    private val _events = Channel<SorteoEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            repository.getCurrent()
                .onSuccess { c -> _uiState.update { it.copy(loading = false, commitment = c) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, loadError = e.message) } }
        }
    }

    fun onPremioChange(value: String) =
        _uiState.update { it.copy(premio = value.take(MAX_PREMIO_LENGTH), submitError = null) }

    fun onSuggestion(value: String) = onPremioChange(value)

    fun onRatifyClick() {
        if (_uiState.value.canRatify) _uiState.update { it.copy(showConfirm = true) }
    }

    fun onDismissConfirm() = _uiState.update { it.copy(showConfirm = false) }

    fun onConfirmRatify() {
        val premio = _uiState.value.premio
        _uiState.update { it.copy(showConfirm = false, submitting = true, submitError = null) }
        viewModelScope.launch {
            repository.ratify(premio)
                .onSuccess { c -> _uiState.update { it.copy(submitting = false, commitment = c) } }
                .onFailure { e -> _uiState.update { it.copy(submitting = false, submitError = e.message) } }
        }
    }

    fun onContinue() {
        viewModelScope.launch { _events.send(SorteoEvent.Ratified) }
    }
}

