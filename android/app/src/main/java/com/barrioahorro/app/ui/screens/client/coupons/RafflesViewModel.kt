package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.raffles.RaffleDto
import com.barrioahorro.app.data.raffles.RaffleRepository
import com.barrioahorro.app.data.raffles.RafflesDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

data class RaffleUi(
    val id: Long,
    val prizeName: String,
    val photoUrl: String?,
    val businessName: String,
    val ticketCost: Int,
    val drawDate: String,
    val totalTickets: Int,
    val participants: Int,
    val myTickets: Int,
    val oneIn: Int,
)

data class RafflesUiState(
    val isLoading: Boolean = true,
    /** Ya hubo al menos una respuesta buena: los refrescos siguientes no muestran spinner. */
    val loaded: Boolean = false,
    val error: String? = null,
    val unlocked: Boolean = false,
    val threshold: Int = 100,
    val lifetimePoints: Int = 0,
    val balance: Int = 0,
    val raffles: List<RaffleUi> = emptyList(),
    /** Se mantiene hasta que termina la animación del candado. */
    val playUnlockAnimation: Boolean = false,
    val confirming: RaffleUi? = null,
    val isBuying: Boolean = false,
    val message: String? = null,
) {
    val missing: Int get() = (threshold - lifetimePoints).coerceAtLeast(0)
}

private val AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
private val AR_LOCALE = Locale("es", "AR")

@HiltViewModel
class RafflesViewModel @Inject constructor(
    private val repository: RaffleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RafflesUiState())
    val state: StateFlow<RafflesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Se llama cada vez que aparece la sección Sorteos. */
    fun refresh() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = !it.loaded, error = null) }
        loadJob = viewModelScope.launch {
            repository.getRaffles()
                .onSuccess { dto -> _state.update { it.merge(dto) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (!it.loaded) e.message else null,
                            message = if (it.loaded) e.message else null,
                        )
                    }
                }
        }
    }

    fun onUnlockAnimationFinished() {
        _state.update { it.copy(playUnlockAnimation = false) }
        viewModelScope.launch { repository.markUnlockSeen() }
    }

    // ---- Compra de tickets ----------------------------------------------------------

    fun requestBuy(raffle: RaffleUi) {
        _state.update { it.copy(confirming = raffle) }
    }

    fun dismissBuy() {
        if (_state.value.isBuying) return
        _state.update { it.copy(confirming = null) }
    }

    fun confirmBuy() {
        val raffle = _state.value.confirming ?: return
        if (_state.value.isBuying) return
        _state.update { it.copy(isBuying = true) }

        viewModelScope.launch {
            repository.buyTicket(raffle.id)
                .onSuccess { result ->
                    val updated = toUi(result.raffle)
                    val tickets = if (updated.myTickets == 1) "1 ticket" else "${updated.myTickets} tickets"
                    _state.update { s ->
                        s.copy(
                            isBuying = false,
                            confirming = null,
                            balance = result.balance,
                            raffles = s.raffles.map { if (it.id == updated.id) updated else it },
                            message = "¡Listo! Ya tenés $tickets para ${updated.prizeName}",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isBuying = false, confirming = null, message = e.message) }
                    // Saldo o sorteo pudieron cambiar: se actualiza lo que se ve.
                    refresh()
                }
        }
    }

    fun onMyTicketsClick() {
        _state.update { it.copy(message = "Próximamente vas a poder ver tus tickets") }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    // ---- Mapeo ----------------------------------------------------------------------

    private fun RafflesUiState.merge(dto: RafflesDto) = copy(
        isLoading = false,
        loaded = true,
        error = null,
        unlocked = dto.unlocked,
        threshold = dto.unlockThreshold,
        lifetimePoints = dto.lifetimePoints,
        balance = dto.balance,
        raffles = dto.raffles.map(::toUi),
        playUnlockAnimation = playUnlockAnimation || dto.showUnlockAnimation,
    )

    private fun toUi(dto: RaffleDto) = RaffleUi(
        id = dto.id,
        prizeName = dto.prizeName,
        photoUrl = dto.prizePhotoUrl,
        businessName = dto.businessName ?: "Comercio del barrio",
        ticketCost = dto.ticketCost,
        drawDate = formatDay(dto.drawAt),
        totalTickets = dto.totalTickets,
        participants = dto.participants,
        myTickets = dto.myTickets,
        oneIn = dto.oneIn,
    )

    private fun formatDay(raw: String): String = runCatching {
        OffsetDateTime.parse(raw).atZoneSameInstant(AR_ZONE)
            .format(DateTimeFormatter.ofPattern("d 'de' MMMM", AR_LOCALE))
    }.getOrDefault(raw)
}