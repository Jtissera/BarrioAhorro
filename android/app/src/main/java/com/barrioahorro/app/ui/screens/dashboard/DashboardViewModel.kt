package com.barrioahorro.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.domain.model.BusinessDashboard
import com.barrioahorro.app.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: BusinessDashboard) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val businessApiService: BusinessApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            _uiState.value = repository.getDashboard().fold(
                onSuccess = { DashboardUiState.Success(it) },
                onFailure = { DashboardUiState.Error(it.message ?: "No se pudo cargar el panel") }
            )
        }
        viewModelScope.launch { loadCategoryName() }
    }

    private suspend fun loadCategoryName() {
        try {
            val response = businessApiService.getMyBusiness()
            if (response.isSuccessful) {
                _categoryName.value = response.body()?.categoryName.orEmpty()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Se deja el valor anterior.
        }
    }
}