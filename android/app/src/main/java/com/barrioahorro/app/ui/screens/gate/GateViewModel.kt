package com.barrioahorro.app.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.cuponera.CuponeraRepository
import com.barrioahorro.app.data.remote.api.BusinessApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** CUPONERA ya no se usa: la ratificación pendiente se resuelve desde RESTRICTED. */
enum class GateDestination { ONBOARDING, CUPONERA, HOME, RESTRICTED, SESSION_EXPIRED }

sealed interface GateUiState {
    data object Checking : GateUiState
    data class Failed(val message: String) : GateUiState
    data class Resolved(val destination: GateDestination) : GateUiState
}

/** Decide a dónde va un comercio al entrar: sin onboarding o con aportes sin ratificar queda limitado. */
@HiltViewModel
class GateViewModel @Inject constructor(
    private val businessApi: BusinessApiService,
    private val cuponeraRepository: CuponeraRepository,
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

        // 2) Cuponera y sorteo ratificados (con margen de gracia): inicio. Si no, acceso limitado.
        val overview = cuponeraRepository.getOverview().getOrElse { error ->
            return GateUiState.Failed(error.message ?: "No pudimos verificar tu aporte.")
        }

        return GateUiState.Resolved(
            if (overview.restrictedAccess) GateDestination.RESTRICTED else GateDestination.HOME,
        )
    }
}