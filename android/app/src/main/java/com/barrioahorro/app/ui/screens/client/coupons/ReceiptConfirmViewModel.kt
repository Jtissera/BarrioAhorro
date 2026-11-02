package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptConfirmState(
    val isLoading: Boolean = true,
    val redemption: RedemptionUi? = null,
    val error: String? = null,
    val isConfirming: Boolean = false,
    /** Ya respondió antes: la pantalla se cierra sola. */
    val answered: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ReceiptConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExploreRepository,
) : ViewModel() {

    private val redemptionId: Long = checkNotNull(savedStateHandle.get<Long>("redemptionId"))

    private val _state = MutableStateFlow(ReceiptConfirmState())
    val state: StateFlow<ReceiptConfirmState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getRedemption(redemptionId)
                .onSuccess { dto ->
                    val answered = repository.getFeedback(redemptionId).getOrNull()?.answered ?: false
                    _state.update { it.copy(isLoading = false, redemption = dto.toUi(), answered = answered) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun confirm(onDone: () -> Unit) {
        if (_state.value.isConfirming) return
        _state.update { it.copy(isConfirming = true) }
        viewModelScope.launch {
            repository.confirmReceipt(redemptionId)
                .onSuccess {
                    _state.update { it.copy(isConfirming = false) }
                    onDone()
                }
                .onFailure { e -> _state.update { it.copy(isConfirming = false, message = e.message) } }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }
}