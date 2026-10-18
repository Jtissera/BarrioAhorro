package com.barrioahorro.app.ui.cuponera.scanner

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import com.barrioahorro.app.data.cuponera.ScanRedemptionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScanOutcome {
    VALIDATED,
    ALREADY_USED,
    EXPIRED,
    CANCELLED,
    OTHER_BUSINESS,
    NOT_FOUND,
    INSUFFICIENT_POINTS,
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): ScanOutcome = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

sealed interface ScannerUiState {
    data object Scanning : ScannerUiState
    data object Processing : ScannerUiState
    data class Finished(val outcome: ScanOutcome, val data: ScanRedemptionDto) : ScannerUiState
    data class Failed(val message: String) : ScannerUiState
}

private const val REPEAT_COOLDOWN_MILLIS = 3_000L

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val repository: CuponeraRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private var lastCode: String? = null
    private var cooldownCode: String? = null
    private var cooldownUntil = 0L

    /** Lo llama el analizador de la cámara; puede dispararse varias veces con el mismo QR. */
    fun onCodeScanned(raw: String) {
        val code = raw.trim()
        if (code.isEmpty()) return
        if (code == cooldownCode && SystemClock.elapsedRealtime() < cooldownUntil) return

        val current = _state.value
        if (current !is ScannerUiState.Scanning) return
        if (!_state.compareAndSet(current, ScannerUiState.Processing)) return

        lastCode = code
        submit(code)
    }

    /** Reenvía el mismo código tras un error de red. */
    fun retry() {
        val code = lastCode ?: return
        val current = _state.value
        if (current !is ScannerUiState.Failed) return
        if (!_state.compareAndSet(current, ScannerUiState.Processing)) return
        submit(code)
    }

    fun scanAgain() {
        if (_state.value is ScannerUiState.Finished) {
            cooldownCode = lastCode
            cooldownUntil = SystemClock.elapsedRealtime() + REPEAT_COOLDOWN_MILLIS
        }
        _state.value = ScannerUiState.Scanning
    }

    private fun submit(code: String) {
        viewModelScope.launch {
            repository.validateRedemption(code)
                .onSuccess { data ->
                    _state.value = ScannerUiState.Finished(ScanOutcome.from(data.result), data)
                }
                .onFailure { e ->
                    _state.value = ScannerUiState.Failed(e.message ?: "No pudimos validar el código.")
                }
        }
    }
}