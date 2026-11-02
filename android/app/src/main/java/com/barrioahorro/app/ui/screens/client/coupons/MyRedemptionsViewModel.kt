package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.explore.RedemptionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MyRedemptionsState(
    val isLoading: Boolean = true,
    val items: List<RedemptionUi> = emptyList(),
    val error: String? = null,
    /** Saldo del vecino (chip "380 pts disp."). */
    val points: Int? = null,
    /** Puntos de canjes validados en el mes actual (banner "¡Ahorraste...!"). */
    val spentThisMonth: Int = 0,
    val monthName: String = "",
    /** Canjes validados cuya recepción ya fue respondida. */
    val answeredIds: Set<Long> = emptySet(),
)

private val AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
private val AR_LOCALE = Locale("es", "AR")

@HiltViewModel
class MyRedemptionsViewModel @Inject constructor(
    private val repository: ExploreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyRedemptionsState())
    val state: StateFlow<MyRedemptionsState> = _state.asStateFlow()

    /** Se llama cada vez que la pantalla aparece: al volver del detalle el estado pudo cambiar. */
    fun load() {
        _state.update { it.copy(isLoading = it.items.isEmpty(), error = null) }

        viewModelScope.launch {
            repository.getSummary().onSuccess { s -> _state.update { it.copy(points = s.points) } }
        }

        viewModelScope.launch {
            repository.getRedemptions()
                .onSuccess { list ->
                    val month = YearMonth.now(AR_ZONE)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = list.map { dto -> dto.toUi() },
                            spentThisMonth = spentIn(month, list),
                            monthName = month.month.getDisplayName(TextStyle.FULL, AR_LOCALE),
                        )
                    }
                    loadAnswered(list.filter { it.status == "VALIDADO" }.map { it.id })
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = if (it.items.isEmpty()) e.message else null) }
                }
        }
    }

    private fun spentIn(month: YearMonth, list: List<RedemptionDto>): Int =
        list.filter { it.status == "VALIDADO" }
            .filter { dto ->
                runCatching {
                    val date = OffsetDateTime.parse(dto.validatedAt ?: dto.redeemedAt).atZoneSameInstant(AR_ZONE)
                    YearMonth.from(date) == month
                }.getOrDefault(false)
            }
            .sumOf { it.pointsCost }

    /** Una consulta por canje validado (máx. 30) para saber si ya confirmó la recepción. */
    private suspend fun loadAnswered(ids: List<Long>) {
        val answered = coroutineScope {
            ids.take(30)
                .map { id -> async { id to (repository.getFeedback(id).getOrNull()?.answered == true) } }
                .awaitAll()
        }.filter { it.second }.map { it.first }.toSet()
        _state.update { it.copy(answeredIds = answered) }
    }
}