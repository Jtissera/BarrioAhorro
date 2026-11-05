package com.barrioahorro.app.ui.screens.client.coupons

import com.barrioahorro.app.data.raffles.MyRaffleTicketsDto
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.raffles.RaffleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class MyTicketStatus { ACTIVE, WON, REDEEMED, EXPIRED, NOT_WON, REFUNDED }

enum class MyTicketsTab { ACTIVOS, RESULTADOS }



data class MyTicketUi(
    val raffleId: Long,
    val status: MyTicketStatus,
    val prizeName: String,
    val description: String?,
    val businessName: String,
    val drawDate: String,
    val tickets: Int,
    val numbersLabel: String,
    val winningTicketId: Long?,
)

data class MyTicketsState(
    val isLoading: Boolean = true,
    val items: List<MyTicketUi> = emptyList(),
    val tab: MyTicketsTab = MyTicketsTab.ACTIVOS,
    val error: String? = null,
) {
    val active: List<MyTicketUi> get() = items.filter { it.status == MyTicketStatus.ACTIVE }
    val results: List<MyTicketUi> get() = items.filter { it.status != MyTicketStatus.ACTIVE }
}

private val AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
private val AR_LOCALE = Locale("es", "AR")

@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    private val repository: RaffleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyTicketsState())
    val state: StateFlow<MyTicketsState> = _state.asStateFlow()

    /** Se llama cada vez que aparece la pantalla: un sorteo pudo haberse ejecutado. */
    fun load() {
        _state.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            repository.getMyTickets()
                .onSuccess { dto ->
                    _state.update {
                        it.copy(isLoading = false, items = dto.items.map(::toUi))
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = if (it.items.isEmpty()) e.message else null)
                    }
                }
        }
    }

    fun setTab(tab: MyTicketsTab) {
        _state.update { it.copy(tab = tab) }
    }

    private fun toUi(dto: MyRaffleTicketsDto) = MyTicketUi(
        raffleId = dto.raffleId,
        status = when {
            dto.won -> when (dto.prizeStatus) {
                "CANJEADO" -> MyTicketStatus.REDEEMED
                "VENCIDO" -> MyTicketStatus.EXPIRED
                else -> MyTicketStatus.WON
            }
            dto.raffleStatus == "ACTIVO" || dto.raffleStatus == "CERRADO" -> MyTicketStatus.ACTIVE
            dto.raffleStatus == "ANULADO" -> MyTicketStatus.REFUNDED
            else -> MyTicketStatus.NOT_WON
        },
        prizeName = dto.prizeName,
        description = dto.prizeDescription?.takeIf { it.isNotBlank() },
        businessName = dto.businessName ?: "Comercio del barrio",
        drawDate = formatDay(dto.drawAt),
        tickets = dto.myTickets,
        numbersLabel = numbersLabel(dto.ticketNumbers),
        winningTicketId = dto.winningTicketId,
    )

    private fun numbersLabel(numbers: List<Long>): String {
        if (numbers.isEmpty()) return ""
        val prefix = if (numbers.size == 1) "Número: " else "Números: "
        val shown = numbers.take(3).joinToString(" · ") { "#$it" }
        val extra = if (numbers.size > 3) " +${numbers.size - 3}" else ""
        return prefix + shown + extra
    }

    private fun formatDay(raw: String): String = runCatching {
        OffsetDateTime.parse(raw).atZoneSameInstant(AR_ZONE)
            .format(DateTimeFormatter.ofPattern("d 'de' MMMM", AR_LOCALE))
    }.getOrDefault(raw)
}