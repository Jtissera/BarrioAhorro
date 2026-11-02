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

enum class ReportReason(val code: String, val label: String) {
    INCOMPLETE("PEDIDO_INCOMPLETO", "Pedido incompleto"),
    QUALITY("CALIDAD", "Calidad del producto"),
    SCHEDULE("HORARIO", "Horario de retiro"),
    OTHER("OTRO", "Otro"),
}

val REPORT_SUGGESTIONS = listOf("Faltan productos", "Estado del producto", "Demora en entrega")

const val REPORT_MAX_LENGTH = 300

data class ReportState(
    val redemption: RedemptionUi? = null,
    val reason: ReportReason? = null,
    val description: String = "",
    val showEmptyError: Boolean = false,
    val isSending: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ReportRedemptionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExploreRepository,
) : ViewModel() {

    private val redemptionId: Long = checkNotNull(savedStateHandle.get<Long>("redemptionId"))

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRedemption(redemptionId)
                .onSuccess { dto -> _state.update { it.copy(redemption = dto.toUi()) } }
        }
    }

    fun selectReason(reason: ReportReason) {
        _state.update { it.copy(reason = if (it.reason == reason) null else reason) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value.take(REPORT_MAX_LENGTH), showEmptyError = false) }
    }

    fun applySuggestion(text: String) {
        _state.update {
            val joined = if (it.description.isBlank()) text else "${it.description.trimEnd()}. $text"
            it.copy(description = joined.take(REPORT_MAX_LENGTH), showEmptyError = false)
        }
    }

    fun submit(onDone: () -> Unit) {
        val current = _state.value
        if (current.isSending) return
        if (current.description.isBlank()) {
            _state.update { it.copy(showEmptyError = true) }
            return
        }
        _state.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repository.reportRedemption(
                id = redemptionId,
                reason = (current.reason ?: ReportReason.OTHER).code,
                description = current.description.trim(),
            )
                .onSuccess {
                    _state.update { it.copy(isSending = false) }
                    onDone()
                }
                .onFailure { e -> _state.update { it.copy(isSending = false, message = e.message) } }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }
}