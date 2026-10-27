package com.barrioahorro.app.ui.screens.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.admin.AdminPublicationDto
import com.barrioahorro.app.data.admin.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val REJECT_MIN = 15
const val REJECT_MAX = 300

enum class AdminFilter { ALL, CUPON, SORTEO }

data class AdminListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<AdminPublicationDto> = emptyList(),
    val filter: AdminFilter = AdminFilter.ALL,
) {
    val visible: List<AdminPublicationDto>
        get() = when (filter) {
            AdminFilter.ALL -> items
            AdminFilter.CUPON -> items.filter { it.kind == "CUPON" }
            AdminFilter.SORTEO -> items.filter { it.kind == "SORTEO" }
        }
    val cuponCount get() = items.count { it.kind == "CUPON" }
    val sorteoCount get() = items.count { it.kind == "SORTEO" }
}

@HiltViewModel
class AdminListViewModel @Inject constructor(private val repo: AdminRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminListUiState())
    val uiState: StateFlow<AdminListUiState> = _uiState.asStateFlow()
    private var job: Job? = null

    /** Se llama en ON_RESUME: al volver de revisar una publicación la lista se actualiza sola. */
    fun load() {
        if (job?.isActive == true) return
        _uiState.update { it.copy(loading = it.items.isEmpty(), error = null) }
        job = viewModelScope.launch {
            repo.pending()
                .onSuccess { list -> _uiState.update { it.copy(loading = false, items = list) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun setFilter(filter: AdminFilter) = _uiState.update { it.copy(filter = filter) }
}

data class AdminReviewUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val item: AdminPublicationDto? = null,
    val reason: String = "",
    val submitting: Boolean = false,
    val actionError: String? = null,
) {
    val canReject get() = reason.trim().length >= REJECT_MIN && !submitting
}

/** Lo comparten el detalle y la pantalla de rechazo (cada una tiene su instancia). */
@HiltViewModel
class AdminReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AdminRepository,
) : ViewModel() {

    private val kind: String = savedStateHandle.get<String>("kind").orEmpty()
    private val id: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(AdminReviewUiState())
    val uiState: StateFlow<AdminReviewUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.detail(kind, id)
                .onSuccess { item -> _uiState.update { it.copy(loading = false, item = item) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun onReasonChange(value: String) =
        _uiState.update { it.copy(reason = value.take(REJECT_MAX), actionError = null) }

    fun approve() = act { repo.approve(kind, id) }

    fun reject() {
        if (!_uiState.value.canReject) return
        act { repo.reject(kind, id, _uiState.value.reason.trim()) }
    }

    private fun act(call: suspend () -> Result<Unit>) {
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(submitting = true, actionError = null) }
        viewModelScope.launch {
            call()
                .onSuccess { _done.send(Unit) }
                .onFailure { e -> _uiState.update { it.copy(submitting = false, actionError = e.message) } }
        }
    }
}