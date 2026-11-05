package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.raffles.RaffleRepository
import com.barrioahorro.app.data.raffles.WinnerNoticeDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WinnerNoticeViewModel @Inject constructor(
    private val repository: RaffleRepository,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<WinnerNoticeDto>>(emptyList())
    val queue: StateFlow<List<WinnerNoticeDto>> = _queue.asStateFlow()

    /** Se llama al entrar al home. Si falla, no se molesta al usuario. */
    fun load() {
        viewModelScope.launch {
            repository.getPendingWinnerNotices().onSuccess { list -> _queue.value = list }
        }
    }

    /** Cerrar o abrir el ticket cuenta como "visto": no vuelve a aparecer. */
    fun dismiss(ticketId: Long) {
        _queue.update { list -> list.filterNot { it.ticketId == ticketId } }
        viewModelScope.launch { repository.markWinnerNoticeSeen(ticketId) }
    }
}