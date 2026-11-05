package com.barrioahorro.app.ui.cuponera.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.raffles.PrizeScanResultDto
import com.barrioahorro.app.data.raffles.RaffleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PRIZE_QR_PREFIX = "BA-SORTEO:"

data class PrizeClaimState(
    val loading: Boolean = true,
    val result: PrizeScanResultDto? = null,
    val confirming: Boolean = false,
    val delivered: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PrizeClaimViewModel @Inject constructor(
    private val repository: RaffleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PrizeClaimState())
    val state: StateFlow<PrizeClaimState> = _state.asStateFlow()

    private var code: String = ""

    fun start(scanned: String) {
        code = scanned
        _state.value = PrizeClaimState()
        viewModelScope.launch {
            repository.verifyPrize(scanned)
                .onSuccess { r -> _state.update { it.copy(loading = false, result = r) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun confirm() {
        if (_state.value.confirming) return
        _state.update { it.copy(confirming = true, error = null) }
        viewModelScope.launch {
            repository.redeemPrize(code)
                .onSuccess { r ->
                    _state.update { it.copy(confirming = false, result = r, delivered = r.valid) }
                }
                .onFailure { e -> _state.update { it.copy(confirming = false, error = e.message) } }
        }
    }
}