package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.raffles.PrizeDetailDto
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

enum class PrizeStatus { CLAIMABLE, REDEEMED, EXPIRED }

data class PrizeUi(
    val ticketNumber: Long,
    val prizeName: String,
    val description: String?,
    val photoUrl: String?,
    val businessName: String,
    val status: PrizeStatus,
    val expiresLabel: String,
    val redeemedLabel: String?,
    val qrPayload: String?,
)

data class WinningTicketState(
    val isLoading: Boolean = true,
    val prize: PrizeUi? = null,
    val error: String? = null,
)

private val AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
private val AR_LOCALE = Locale("es", "AR")

@HiltViewModel
class WinningTicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RaffleRepository,
) : ViewModel() {

    private val ticketId: Long = checkNotNull(savedStateHandle["ticketId"])

    private val _state = MutableStateFlow(WinningTicketState())
    val state: StateFlow<WinningTicketState> = _state.asStateFlow()

    /** silent = refresco en segundo plano (sin spinner ni errores). */
    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(isLoading = it.prize == null, error = null) }
        viewModelScope.launch {
            repository.getPrize(ticketId)
                .onSuccess { dto -> _state.update { it.copy(isLoading = false, prize = toUi(dto), error = null) } }
                .onFailure { e ->
                    if (!silent) _state.update { it.copy(isLoading = false, error = if (it.prize == null) e.message else null) }
                }
        }
    }

    private fun toUi(dto: PrizeDetailDto) = PrizeUi(
        ticketNumber = dto.ticketNumber,
        prizeName = dto.prizeName,
        description = dto.prizeDescription,
        photoUrl = dto.prizePhotoUrl,
        businessName = dto.businessName ?: "Comercio del barrio",
        status = when (dto.status) {
            "CANJEADO" -> PrizeStatus.REDEEMED
            "VENCIDO" -> PrizeStatus.EXPIRED
            else -> PrizeStatus.CLAIMABLE
        },
        expiresLabel = format(dto.expiresAt),
        redeemedLabel = dto.redeemedAt?.let(::format),
        qrPayload = dto.qrPayload,
    )

    private fun format(raw: String): String = runCatching {
        OffsetDateTime.parse(raw).atZoneSameInstant(AR_ZONE)
            .format(DateTimeFormatter.ofPattern("d 'de' MMMM", AR_LOCALE))
    }.getOrDefault(raw)
}