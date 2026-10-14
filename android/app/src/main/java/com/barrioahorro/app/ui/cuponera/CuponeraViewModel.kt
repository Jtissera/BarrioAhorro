package com.barrioahorro.app.ui.cuponera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraCommitmentDto
import com.barrioahorro.app.data.cuponera.CuponeraProductRequestDto
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import com.barrioahorro.app.data.cuponera.RatifyCuponeraRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

fun pointsFor(valor: Long, pesosPorPunto: Int): Int =
    maxOf(1L, (valor + pesosPorPunto - 1) / pesosPorPunto).toInt()

/** Producto en edición (todavía no ratificado). */
data class DraftProduct(
    val localId: Long,
    val nombre: String = "",
    val valorDigits: String = "",
    val unidades: Int = 5,
    val fotoUrl: String? = null,
    val fotoSubiendo: Boolean = false,
    val fotoError: String? = null,
) {
    val valor: Long? get() = valorDigits.toLongOrNull()
    val isValid: Boolean
        get() = nombre.isNotBlank() && (valor ?: 0L) > 0L && unidades in 1..9999
}

data class CuponeraUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val commitment: CuponeraCommitmentDto? = null,
    val drafts: List<DraftProduct> = emptyList(),
    val submitting: Boolean = false,
    val submitError: String? = null,
) {
    val minProductos: Int get() = commitment?.minProductos ?: 2
    val maxProductos: Int get() = commitment?.maxProductos ?: 4
    val pesosPorPunto: Int get() = commitment?.pesosPorPunto ?: 500
    val suggestions: List<String> get() = commitment?.sugerencias ?: emptyList()

    /** Ya ratificado, vencido o fuera de plazo: solo se muestra, no se edita. */
    val readOnly: Boolean
        get() {
            val c = commitment ?: return false
            return !(c.estado == "PENDIENTE" && c.puedeRatificar)
        }

    val validCount: Int get() = drafts.count { it.isValid }
    val canAdd: Boolean get() = !readOnly && drafts.size < maxProductos
    val canAddOrFill: Boolean get() = canAdd || (!readOnly && drafts.any { it.nombre.isBlank() })
    val canSubmit: Boolean
        get() = !readOnly &&
                !submitting &&
                drafts.size in minProductos..maxProductos &&
                drafts.all { it.isValid && !it.fotoSubiendo }
}

sealed interface CuponeraEvent {
    data object Ratified : CuponeraEvent
}

@HiltViewModel
class CuponeraViewModel @Inject constructor(
    private val repository: CuponeraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuponeraUiState())
    val uiState: StateFlow<CuponeraUiState> = _uiState.asStateFlow()

    private val _events = Channel<CuponeraEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextLocalId = 1L

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.getCurrent()
                .onSuccess { c ->
                    _uiState.update { s ->
                        val editable = c.estado == "PENDIENTE" && c.puedeRatificar
                        s.copy(
                            loading = false,
                            commitment = c,
                            // Arranca con tantas tarjetas vacías como el mínimo, para completar directo.
                            drafts = when {
                                !editable -> emptyList()
                                s.drafts.isNotEmpty() -> s.drafts
                                else -> List(c.minProductos) { newDraft() }
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar.") }
                }
        }
    }

    fun onNombreChange(id: Long, value: String) =
        updateDraft(id) { it.copy(nombre = value.take(120)) }

    fun onValorChange(id: Long, raw: String) =
        updateDraft(id) { it.copy(valorDigits = raw.filter(Char::isDigit).take(9).trimStart('0')) }

    fun onUnidadesDelta(id: Long, delta: Int) =
        updateDraft(id) { it.copy(unidades = (it.unidades + delta).coerceIn(1, 9999)) }

    fun onRemove(id: Long) {
        _uiState.update { s -> s.copy(drafts = s.drafts.filterNot { it.localId == id }, submitError = null) }
    }

    fun onAdd() {
        _uiState.update { s ->
            if (s.canAdd) s.copy(drafts = s.drafts + newDraft(), submitError = null) else s
        }
    }

    /** Completa la primera tarjeta sin nombre; si no hay, agrega una nueva (si hay lugar). */
    fun onSuggestion(name: String) {
        _uiState.update { s ->
            val blank = s.drafts.firstOrNull { it.nombre.isBlank() }
            when {
                blank != null -> s.copy(
                    drafts = s.drafts.map { if (it.localId == blank.localId) it.copy(nombre = name) else it },
                )
                s.canAdd -> s.copy(drafts = s.drafts + newDraft().copy(nombre = name))
                else -> s
            }
        }
    }

    fun onRatify() {
        val s = _uiState.value
        if (!s.canSubmit) return

        _uiState.update { it.copy(submitting = true, submitError = null) }
        viewModelScope.launch {
            val body = RatifyCuponeraRequestDto(
                productos = s.drafts.map {
                    CuponeraProductRequestDto(
                        fotoUrl = it.fotoUrl,
                        nombreProducto = it.nombre.trim(),
                        valorComercio = (it.valor ?: 0L).toDouble(),
                        unidadesDisponibles = it.unidades,
                    )
                },
            )
            repository.ratify(body)
                .onSuccess { c ->
                    _uiState.update { it.copy(submitting = false, commitment = c, drafts = emptyList()) }
                    _events.trySend(CuponeraEvent.Ratified)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(submitting = false, submitError = e.message) }
                }
        }
    }

    private fun newDraft() = DraftProduct(localId = nextLocalId++)

    private fun updateDraft(id: Long, transform: (DraftProduct) -> DraftProduct) {
        _uiState.update { s ->
            s.copy(
                drafts = s.drafts.map { if (it.localId == id) transform(it) else it },
                submitError = null,
            )
        }
    }

    fun onPhotoCaptured(id: Long, file: File) {
        updateDraft(id) { it.copy(fotoSubiendo = true, fotoError = null) }

        viewModelScope.launch {
            repository.uploadPhoto(file)
                .onSuccess { response ->
                    updateDraft(id) {
                        it.copy(fotoUrl = response.url, fotoSubiendo = false, fotoError = null)
                    }
                }
                .onFailure { error ->
                    //desp lo saco
                    Log.e("Cuponera", "uploadPhoto falló", error)
                    updateDraft(id) {
                        it.copy(
                            fotoSubiendo = false,
                            fotoError = (error.message ?: "No se pudo subir la foto.") +
                                    " Tocá la foto para reintentar.",
                        )
                    }
                }
            file.delete()
        }
    }

}