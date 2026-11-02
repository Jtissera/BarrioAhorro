package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.CouponDto
import com.barrioahorro.app.data.explore.CouponQuery
import com.barrioahorro.app.data.explore.ExploreCategory
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class CouponDistance(val label: String, val km: Double?) {
    ANY("Cualquiera", null),
    UNDER_1("Menos de 1 km", 1.0),
    UNDER_3("Menos de 3 km", 3.0),
    UNDER_5("Menos de 5 km", 5.0),
}

data class CouponUi(
    val id: Long,
    val businessName: String,
    val productName: String,
    val description: String?,
    val photoUrl: String?,
    val pointsCost: Int,
    val validUntil: String,
    val distance: String?,
)

data class CouponsUiState(
    val location: ClientLocation? = null,
    val points: Int? = null,
    val categories: List<ExploreCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val distance: CouponDistance = CouponDistance.ANY,
    val query: String = "",
    val coupons: List<CouponUi> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Cupón que el vecino está por canjear (modal "Confirmar canje"). */
    val confirming: CouponUi? = null,
    val isRedeeming: Boolean = false,
) {
    val hasLocation: Boolean get() = location != null

    val selectedCategory: ExploreCategory? get() = categories.firstOrNull { it.id == selectedCategoryId }

    /** El radio solo cuenta si hay ubicación. */
    val hasActiveFilters: Boolean
        get() = selectedCategoryId != null || query.isNotBlank() ||
                (hasLocation && distance != CouponDistance.ANY)
}

private const val PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 400L

@HiltViewModel
class CouponsViewModel @Inject constructor(
    private val repository: ExploreRepository,
    private val locationStore: ClientLocationDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CouponsUiState())
    val state: StateFlow<CouponsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var shownOnce = false

    init {
        loadCategories()
        refreshSummary()
        // Cada cambio de ubicación (el primer valor incluido) recarga la lista.
        viewModelScope.launch {
            locationStore.location.distinctUntilChanged().collect { location ->
                _state.update { it.copy(location = location) }
                reload()
            }
        }
    }

    /** La primera vez ya carga el init; las siguientes (volver a la pestaña) refrescan sin parpadeo. */
    fun onScreenShown() {
        if (!shownOnce) {
            shownOnce = true
            return
        }
        reload(keepList = true)
        refreshSummary()
    }

    fun refresh() {
        reload()
        refreshSummary()
    }

    fun retry() = reload()

    fun refreshSummary() {
        viewModelScope.launch {
            repository.getSummary().onSuccess { s -> _state.update { it.copy(points = s.points) } }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { list -> _state.update { it.copy(categories = list) } }
        }
    }

    private fun reload(keepList: Boolean = false) {
        loadJob?.cancel()
        _state.update {
            it.copy(
                isLoading = !keepList,
                isLoadingMore = false,
                error = null,
                coupons = if (keepList) it.coupons else emptyList(),
            )
        }
        val query = _state.value.toQuery()

        loadJob = viewModelScope.launch {
            repository.getCoupons(query, page = 0, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            coupons = page.items.map(::toUi),
                            page = 0,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.coupons.isEmpty()) e.message else null,
                            message = if (it.coupons.isNotEmpty()) e.message else null,
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return

        val nextPage = current.page + 1
        val query = current.toQuery()
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch {
            repository.getCoupons(query, page = nextPage, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            coupons = (it.coupons + page.items.map(::toUi)).distinctBy { c -> c.id },
                            page = nextPage,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, message = e.message) } }
        }
    }

    // ---- Canje ----------------------------------------------------------------------

    fun requestRedeem(coupon: CouponUi) {
        _state.update { it.copy(confirming = coupon) }
    }

    fun dismissRedeem() {
        if (_state.value.isRedeeming) return
        _state.update { it.copy(confirming = null) }
    }

    /** Si sale bien, [onDone] recibe el id del canje para abrir "Tu canje" con el QR. */
    fun confirmRedeem(onDone: (Long) -> Unit) {
        val coupon = _state.value.confirming ?: return
        if (_state.value.isRedeeming) return
        _state.update { it.copy(isRedeeming = true) }

        viewModelScope.launch {
            repository.redeemCoupon(coupon.id)
                .onSuccess { redemption ->
                    _state.update { it.copy(isRedeeming = false, confirming = null) }
                    onDone(redemption.id)
                }
                .onFailure { e ->
                    _state.update { it.copy(isRedeeming = false, confirming = null, message = e.message) }
                    // Stock o saldo pudieron cambiar: se actualiza lo que se ve.
                    reload(keepList = true)
                    refreshSummary()
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

    fun selectCategory(id: Int?) {
        _state.update { it.copy(selectedCategoryId = id) }
        reload()
    }

    fun selectDistance(distance: CouponDistance) {
        _state.update { it.copy(distance = distance) }
        reload()
    }

    fun clearFilters() {
        searchJob?.cancel()
        _state.update { it.copy(selectedCategoryId = null, distance = CouponDistance.ANY, query = "") }
        reload()
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    // ---- Mapeo ----------------------------------------------------------------------

    private fun CouponsUiState.toQuery() = CouponQuery(
        latitude = location?.latitude,
        longitude = location?.longitude,
        radiusKm = if (location != null) distance.km else null,
        categoryIds = listOfNotNull(selectedCategoryId),
        text = query.trim().takeIf { it.isNotEmpty() },
        order = "DISTANCE",
    )

    private fun toUi(dto: CouponDto) = CouponUi(
        id = dto.id,
        businessName = dto.businessName ?: "Comercio del barrio",
        productName = dto.productName,
        description = dto.description,
        pointsCost = dto.pointsCost,
        validUntil = formatDate(dto.validUntil),
        distance = dto.distanceMeters?.let(::formatDistance),
        photoUrl = dto.photoUrl,
    )

    private fun formatDate(raw: String): String = runCatching {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "AR")))
    }.getOrDefault(raw)

    private fun formatDistance(meters: Int): String =
        if (meters < 1000) "a $meters m" else "a ${"%.1f".format(Locale("es", "AR"), meters / 1000.0)} km"
}