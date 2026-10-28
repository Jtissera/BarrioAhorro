package com.barrioahorro.app.ui.cuponera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import com.barrioahorro.app.data.cuponera.RaffleCommitmentDto
import com.barrioahorro.app.data.cuponera.RaffleCycleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.io.File

const val RAFFLE_NAME_MAX_LENGTH = 120
const val RAFFLE_DESCRIPTION_MAX_LENGTH = 255
private const val VALUE_MAX_LENGTH = 12
private const val STATUS_RATIFIED = "RATIFICADO"

data class RaffleRatifyUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    /** Ciclo que se va a ratificar (el próximo, o el actual si no hay próximo). */
    val target: RaffleCommitmentDto? = null,
    val canRatify: Boolean = false,
    /** Compromiso ya ratificado: si no es null se muestra el estado "¡Todo listo!". */
    val ratified: RaffleCommitmentDto? = null,
    val showConfirm: Boolean = false,
    val name: String = "",
    val description: String = "",
    val valueText: String = "",
    val photoUrl: String? = null,
    val photoUploading: Boolean = false,
    val photoError: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val value: Double? get() = valueText.toDoubleOrNull()?.takeIf { it > 0 }

    val canSubmit: Boolean
        get() = canRatify && name.isNotBlank() && description.isNotBlank() &&
                value != null && !submitting && !loading && !photoUploading
}

@HiltViewModel
class RaffleRatifyViewModel @Inject constructor(
    private val repository: CuponeraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaffleRatifyUiState())
    val uiState: StateFlow<RaffleRatifyUiState> = _uiState.asStateFlow()

    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            repository.getOverview()
                .onSuccess { overview -> applyCycle(overview.raffle) }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, loadError = e.message ?: "No pudimos cargar el sorteo") }
                }
        }
    }

    private fun applyCycle(cycle: RaffleCycleDto) {
        val ratified = if (cycle.canRatify) null else cycle.ratifiedCommitment()
        _uiState.update {
            it.copy(
                loading = false,
                target = cycle.next ?: cycle.current,
                canRatify = cycle.canRatify,
                ratified = ratified,
            )
        }
    }

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

    fun onRequestConfirm() {
        if (_uiState.value.canSubmit) _uiState.update { it.copy(showConfirm = true) }
    }

    fun onDismissConfirm() {
        _uiState.update { it.copy(showConfirm = false) }
    }

    fun onPhotoCaptured(file: File) {
        _uiState.update { it.copy(photoUploading = true, photoError = null) }
        viewModelScope.launch {
            repository.uploadPhoto(file)
                .onSuccess { response ->
                    _uiState.update { it.copy(photoUrl = response.url, photoUploading = false, photoError = null) }
                }
                .onFailure { error ->
                    Log.e("RaffleRatify", "uploadPhoto falló", error)
                    _uiState.update {
                        it.copy(
                            photoUploading = false,
                            photoError = (error.message ?: "No se pudo subir la foto.") +
                                    " Tocá la foto para reintentar.",
                        )
                    }
                }
            file.delete()
        }
    }

    fun submit() {
        val state = _uiState.value
        val value = state.value
        if (!state.canSubmit || value == null) return

        _uiState.update { it.copy(showConfirm = false, submitting = true, error = null) }
        viewModelScope.launch {
            repository.ratifyRaffle(state.name.trim(), state.description.trim(), value, state.photoUrl)                .onSuccess { cycle ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            canRatify = false,
                            ratified = cycle.ratifiedCommitment() ?: it.target,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(submitting = false, error = e.message) }
                }
        }
    }

    fun onContinue() {
        _events.trySend(Unit)
    }

    private fun RaffleCycleDto.ratifiedCommitment(): RaffleCommitmentDto? =
        next?.takeIf { it.status == STATUS_RATIFIED } ?: current?.takeIf { it.status == STATUS_RATIFIED }
}