package com.barrioahorro.app.ui.screens.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusinessProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val businessName: String = "",
    val categoryName: String = "",
    val direccion: String = "",
    val descripcion: String = "",
    val coverUrl: String? = null,
    val schedule: List<DayScheduleUi> = emptyList(),
)

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState

    private var loaded = false

    /** Se llama en ON_RESUME: carga al entrar y al volver de las pantallas de edición. */
    fun load() {
        viewModelScope.launch {
            // Si ya hay datos, recarga sin spinner.
            if (!loaded) _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = businessApiService.getMyBusiness()
                val business = response.body()
                if (!response.isSuccessful || business == null) {
                    if (!loaded) _uiState.update { it.copy(isLoading = false, error = "No pudimos cargar tu perfil") }
                    return@launch
                }
                loaded = true
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        businessName = business.businessName.orEmpty(),
                        categoryName = business.categoryName.orEmpty(),
                        direccion = business.direccion.orEmpty(),
                        descripcion = business.descripcion.orEmpty(),
                        coverUrl = business.fotos.toPhotoUi(BASE_URL).firstOrNull()?.url,
                        schedule = business.horarios.toScheduleUi(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!loaded) _uiState.update { it.copy(isLoading = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }
}