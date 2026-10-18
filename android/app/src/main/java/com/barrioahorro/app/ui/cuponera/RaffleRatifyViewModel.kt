package com.barrioahorro.app.ui.cuponera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val RAFFLE_NAME_MAX_LENGTH = 120
const val RAFFLE_DESCRIPTION_MAX_LENGTH = 255
private const val VALUE_MAX_LENGTH = 12

data class RaffleRatifyUiState(
    val name: String = "",
    val description: String = "",
    val valueText: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val value: Double? get() = valueText.toDoubleOrNull()?.takeIf { it > 0 }

    val canSubmit: Boolean
        get() = name.isNotBlank() && description.isNotBlank() && value != null && !submitting
}

@HiltViewModel
class RaffleRatifyViewModel @Inject constructor(
    private val repository: CuponeraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaffleRatifyUiState())
    val uiState: StateFlow<RaffleRatifyUiState> = _uiState.asStateFlow()

    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value.take(RAFFLE_NAME_MAX_LENGTH), error = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value.take(RAFFLE_DESCRIPTION_MAX_LENGTH), error = null) }
    }

    /** Acepta solo dígitos y un separador decimal, con hasta 2 decimales. */
    fun onValueChange(raw: String) {
        val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
        val parts = cleaned.split('.')
        if (parts.size > 2) return
        if (parts.getOrNull(1)?.length ?: 0 > 2) return
        if (cleaned.length > VALUE_MAX_LENGTH) return
        _uiState.update { it.copy(valueText = cleaned, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        val value = state.value
        if (!state.canSubmit || value == null) return

        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            repository.ratifyRaffle(state.name.trim(), state.description.trim(), value)
                .onSuccess {
                    _uiState.update { it.copy(submitting = false) }
                    _events.trySend(Unit)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(submitting = false, error = e.message) }
                }
        }
    }
}