package com.barrioahorro.app.ui.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.ui.components.OfferPriceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OfferTabFilter {
    VIGENTES, FINALIZADAS
}

data class MyOfferItemUi(
    val id: Long,
    val productName: String,
    val photoUrl: String?,
    val badgeText: String,
    val isYellowBadge: Boolean = false,
    val priceInfo: OfferPriceInfo,
    val businessName: String,
    val categoryName: String,
    val distanceText: String = "a 250 m",
    val vigenciaText: String,
    val confirmedCountText: String = "0 vecinos confirmaron",
    val isVigente: Boolean,
    val isActiva: Boolean,
)

data class MyOffersUiState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val categoryName: String = "",
    val selectedTab: OfferTabFilter = OfferTabFilter.VIGENTES,
    val allOffers: List<MyOfferItemUi> = emptyList(),
    val activeCount: Int = 0,
    val finishedCount: Int = 0,
    val filteredOffers: List<MyOfferItemUi> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MyOffersViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val offerRepository: IOfferRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyOffersUiState())
    val uiState: StateFlow<MyOffersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value.allOffers.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            // 1. Info del comercio
            var bName = _uiState.value.businessName
            var catName = _uiState.value.categoryName
            runCatching {
                val bRes = businessApiService.getMyBusiness()
                if (bRes.isSuccessful) {
                    val b = bRes.body()
                    bName = b?.businessName.orEmpty()
                    catName = b?.categoryName.orEmpty()
                }
            }

            // 2. Ofertas del comercio
            when (val offersResult = offerRepository.getMyOffers()) {
                is Result.Success -> {
                    val rawOffers = offersResult.value
                    val mapped = rawOffers.map { dto ->
                        OfferUiMapper.toUi(dto, bName, catName) // CAMBIA: antes mapOfferDtoToUi(...)
                    }

                    val active = mapped.filter { it.isVigente && it.isActiva }
                    val finished = mapped.filter { !it.isVigente || !it.isActiva }

                    _uiState.update { current ->
                        val currentTab = current.selectedTab
                        current.copy(
                            isLoading = false,
                            businessName = bName,
                            categoryName = catName,
                            allOffers = mapped,
                            activeCount = active.size,
                            finishedCount = finished.size,
                            filteredOffers = if (currentTab == OfferTabFilter.VIGENTES) active else finished,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            businessName = bName,
                            categoryName = catName,
                            errorMessage = "No se pudieron cargar las ofertas",
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: OfferTabFilter) {
        _uiState.update { current ->
            val list = if (tab == OfferTabFilter.VIGENTES) {
                current.allOffers.filter { it.isVigente && it.isActiva }
            } else {
                current.allOffers.filter { !it.isVigente || !it.isActiva }
            }
            current.copy(
                selectedTab = tab,
                filteredOffers = list,
            )
        }
    }
}