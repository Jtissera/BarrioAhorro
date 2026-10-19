package com.barrioahorro.app.ui.screens.client.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreCategory
import com.barrioahorro.app.data.explore.ExploreQuery
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.local.datastore.ClientLocation
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExploreMode { MAP, LIST }

enum class DistanceFilter(val label: String, val km: Double?) {
    UNDER_1("Menos de 1 km", 1.0),
    UNDER_3("Menos de 3 km", 3.0),
    ANY("Cualquiera", null),
}

enum class ExploreOfferType(val label: String, val apiValue: String) {
    PERCENT("Descuento %", "PORCENTAJE"),
    TWO_FOR_ONE("2x1", "DOS_POR_UNO"),
    SECOND_UNIT("2da al 50%", "CANTIDAD_ULTIMA_UNIDAD"),
    BULK("Precio por cantidad / combo", "POR_MAYOR"),
}

enum class ExploreSort(val label: String, val apiValue: String) {
    NEAREST("Más cerca", "DISTANCE"),
    CONFIRMATIONS("Más confirmadas", "CONFIRMATIONS"),
    RECENT("Más recientes", "RECENT"),
}

val DEFAULT_DISTANCE = DistanceFilter.UNDER_3

data class ExploreUiState(
    val location: ClientLocation? = null,
    val points: Int? = null,
    val categories: List<ExploreCategory> = emptyList(),
    val selectedCategoryIds: Set<Int> = emptySet(),
    val selectedTypes: Set<ExploreOfferType> = emptySet(),
    val distance: DistanceFilter = DEFAULT_DISTANCE,
    val query: String = "",
    val sort: ExploreSort = ExploreSort.NEAREST,
    val mode: ExploreMode = ExploreMode.LIST,
    val offers: List<ExploreOfferUi> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val hasLocation: Boolean get() = location != null

    /** Sin ubicación no hay distancias: "Más cerca" cae a "Más recientes". */
    val effectiveSort: ExploreSort
        get() = if (sort == ExploreSort.NEAREST && !hasLocation) ExploreSort.RECENT else sort

    val availableSorts: List<ExploreSort>
        get() = ExploreSort.entries.filter { it != ExploreSort.NEAREST || hasLocation }

    val activeFilterCount: Int
        get() = selectedCategoryIds.size + selectedTypes.size + if (hasLocation && distance != DEFAULT_DISTANCE) 1 else 0

    val hasActiveFilters: Boolean get() = activeFilterCount > 0 || query.isNotBlank()
}

private const val PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 400L

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ExploreRepository,
    private val locationStore: ClientLocationDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private val favoritesInFlight = mutableSetOf<Long>()

    init {
        loadCategories()
        // Cada cambio de ubicación (primer valor incluido) recarga la lista.
        viewModelScope.launch {
            locationStore.location.distinctUntilChanged().collect { location ->
                _state.update { it.copy(location = location) }
                reload()
            }
        }
    }

    // ---- Carga ----------------------------------------------------------------------

    fun refresh() {
        reload(refreshing = true)
        refreshSummary()
    }

    fun retry() = reload()

    fun refreshSummary() {
        viewModelScope.launch {
            repository.getSummary().onSuccess { summary ->
                _state.update { it.copy(points = summary.points) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { list ->
                _state.update { it.copy(categories = list) }
            }
        }
    }

    private fun reload(refreshing: Boolean = false) {
        loadJob?.cancel()
        _state.update {
            it.copy(
                isLoading = !refreshing,
                isRefreshing = refreshing,
                isLoadingMore = false,
                error = null,
                offers = if (refreshing) it.offers else emptyList(),
            )
        }
        val query = _state.value.toQuery()

        loadJob = viewModelScope.launch {
            repository.getOffers(query, page = 0, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            offers = page.items.map(ExploreOfferMapper::toUi),
                            total = page.total,
                            page = 0,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = if (it.offers.isEmpty()) e.message else null,
                            message = if (it.offers.isNotEmpty()) e.message else null,
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isRefreshing || current.isLoadingMore) return

        val nextPage = current.page + 1
        val query = current.toQuery()
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch {
            repository.getOffers(query, page = nextPage, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            offers = (it.offers + page.items.map(ExploreOfferMapper::toUi)).distinctBy { o -> o.id },
                            page = nextPage,
                            total = page.total,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMore = false, message = e.message) }
                }
        }
    }

    // ---- Búsqueda y filtros ---------------------------------------------------------

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value.take(80)) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            reload()
        }
    }

    fun toggleCategory(id: Int) {
        _state.update {
            val next = if (id in it.selectedCategoryIds) it.selectedCategoryIds - id else it.selectedCategoryIds + id
            it.copy(selectedCategoryIds = next)
        }
        reload()
    }

    fun clearCategories() {
        _state.update { it.copy(selectedCategoryIds = emptySet()) }
        reload()
    }

    fun applyFilters(categoryIds: Set<Int>, types: Set<ExploreOfferType>, distance: DistanceFilter) {
        _state.update { it.copy(selectedCategoryIds = categoryIds, selectedTypes = types, distance = distance) }
        reload()
    }

    fun clearAllFilters() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                selectedCategoryIds = emptySet(),
                selectedTypes = emptySet(),
                distance = DEFAULT_DISTANCE,
                query = "",
            )
        }
        reload()
    }

    fun onSortSelected(sort: ExploreSort) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        reload()
    }

    fun onModeChange(mode: ExploreMode) {
        _state.update { it.copy(mode = mode) }
    }

    // ---- Favoritos ------------------------------------------------------------------

    fun toggleFavorite(offerId: Long) {
        val offer = _state.value.offers.firstOrNull { it.id == offerId } ?: return
        if (!favoritesInFlight.add(offerId)) return

        val target = !offer.favorite
        setFavoriteLocally(offerId, target)

        viewModelScope.launch {
            repository.setFavorite(offerId, target).onFailure { e ->
                setFavoriteLocally(offerId, !target)
                _state.update { it.copy(message = e.message) }
            }
            favoritesInFlight.remove(offerId)
        }
    }

    private fun setFavoriteLocally(offerId: Long, favorite: Boolean) {
        _state.update { s ->
            s.copy(offers = s.offers.map { if (it.id == offerId) it.copy(favorite = favorite) else it })
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun ExploreUiState.toQuery() = ExploreQuery(
        latitude = location?.latitude,
        longitude = location?.longitude,
        radiusKm = if (location != null) distance.km else null,
        categoryIds = selectedCategoryIds.sorted(),
        types = selectedTypes.map { it.apiValue },
        text = query.trim().takeIf { it.isNotEmpty() },
        order = effectiveSort.apiValue,
    )
}