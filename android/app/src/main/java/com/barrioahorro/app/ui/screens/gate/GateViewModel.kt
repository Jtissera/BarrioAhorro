package com.barrioahorro.app.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.sorteo.SorteoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

enum class GateDestination { ONBOARDING, CUPONERA, SORTEO, HOME, SESSION_EXPIRED }

sealed interface GateUiState {
    data object Checking : GateUiState
    data class Failed(val message: String) : GateUiState
    data class Resolved(val destination: GateDestination) : GateUiState
}

/** Decide a dónde va un comercio al entrar: nunca llega al inicio con el onboarding o el aporte pendientes. */
@HiltViewModel
class GateViewModel @Inject constructor(
    private val businessApi: BusinessApiService,
    private val cuponeraRepository: CuponeraRepository,
    private val sorteoRepository: SorteoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<GateUiState>(GateUiState.Checking)
    val state: StateFlow<GateUiState> = _state.asStateFlow()

    init {
        check()
    }

    fun check() {
        _state.value = GateUiState.Checking
        viewModelScope.launch { _state.value = resolve() }
    }

    private suspend fun resolve(): GateUiState {
        val response = try {
            businessApi.getMyBusiness()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return GateUiState.Failed("No hay conexión. Revisá tu internet e intentá de nuevo.")
        } catch (e: Exception) {
            return GateUiState.Failed("Ocurrió un error inesperado. Intentá de nuevo.")
        }

        if (response.code() == 401) return GateUiState.Resolved(GateDestination.SESSION_EXPIRED)

        val business = response.body()
        if (!response.isSuccessful || business == null) {
            return GateUiState.Failed("No pudimos verificar tu cuenta (${response.code()}). Intentá de nuevo.")
        }

        // 1) Sin onboarding completo no se entra a la app.
        if (!business.onboardingCompleted) return GateUiState.Resolved(GateDestination.ONBOARDING)

        // 2) Solo con el aporte ratificado se entra al inicio; pendiente o vencido se queda en la cuponera.
        val commitment = cuponeraRepository.getCurrent().getOrElse { error ->
            return GateUiState.Failed(error.message ?: "No pudimos verificar tu aporte a la cuponera.")
        }

        if (commitment.estado != "RATIFICADO") return GateUiState.Resolved(GateDestination.CUPONERA)

        // 3) Con la cuponera ratificada, falta el premio del sorteo del ciclo.
        val sorteo = sorteoRepository.getCurrent().getOrElse { error ->
            return GateUiState.Failed(error.message ?: "No pudimos verificar tu premio del sorteo.")
        }

        return GateUiState.Resolved(
            if (sorteo.estado == "RATIFICADO") GateDestination.HOME else GateDestination.SORTEO,
        )
    }
}