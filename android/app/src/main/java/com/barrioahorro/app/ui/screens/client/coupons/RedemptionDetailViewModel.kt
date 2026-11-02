package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RedemptionDetailState(
    val isLoading: Boolean = true,
    val redemption: RedemptionUi? = null,
    val error: String? = null,
    val isCancelling: Boolean = false,
    val message: String? = null,
    val points: Int? = null,
    val feedbackAnswered: Boolean? = null,
    val promptReceipt: Boolean = false,
)

private const val POLL_MILLIS = 5_000L

@HiltViewModel
class RedemptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExploreRepository,
) : ViewModel() {

    private val redemptionId: Long = checkNotNull(savedStateHandle.get<Long>("redemptionId"))

    private val _state = MutableStateFlow(RedemptionDetailState())
    val state: StateFlow<RedemptionDetailState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repository.getSummary().onSuccess { s -> _state.update { it.copy(points = s.points) } }
        }
        viewModelScope.launch {
            while (true) {
                delay(POLL_MILLIS)
                if (_state.value.redemption?.status == RedemptionStatus.PENDING && !_state.value.isCancelling) {
                    fetch(silent = true)
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch { fetch(silent = false) }
    }

    private suspend fun fetch(silent: Boolean) {
        if (!silent) _state.update { it.copy(isLoading = it.redemption == null, error = null) }
        repository.getRedemption(redemptionId)
            .onSuccess { dto ->
                val ui = dto.toUi()
                val previous = _state.value.redemption?.status
                _state.update { it.copy(isLoading = false, redemption = ui, error = null) }
                if (ui.status == RedemptionStatus.VALIDATED) {
                    loadFeedback(promptIfUnanswered = previous == RedemptionStatus.PENDING)
                }
            }
            .onFailure { e ->
                if (!silent) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.redemption == null) e.message else null,
                            message = if (it.redemption != null) e.message else null,
                        )
                    }
                }
            }
    }

    private suspend fun loadFeedback(promptIfUnanswered: Boolean) {
        repository.getFeedback(redemptionId).onSuccess { f ->
            _state.update {
                it.copy(
                    feedbackAnswered = f.answered,
                    promptReceipt = it.promptReceipt || (promptIfUnanswered && !f.answered),
                )
            }
        }
    }

    fun onPromptConsumed() {
        _state.update { it.copy(promptReceipt = false) }
    }

    fun cancel() {
        if (_state.value.isCancelling) return
        _state.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            repository.cancelRedemption(redemptionId)
                .onSuccess { dto ->
                    _state.update { it.copy(isCancelling = false, redemption = dto.toUi(), message = "Canje cancelado") }
                }
                .onFailure { e ->
                    _state.update { it.copy(isCancelling = false, message = e.message) }
                    fetch(silent = true)
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }
}