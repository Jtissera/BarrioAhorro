package com.barrioahorro.app.ui.screens.client.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoritesFilter { ALL, LIVE, ENDED }

data class FavoritesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val items: List<ExploreOfferUi> = emptyList(),
    val filter: FavoritesFilter = FavoritesFilter.ALL,
) {
    val visible: List<ExploreOfferUi>
        get() = when (filter) {
            FavoritesFilter.ALL -> items
            FavoritesFilter.LIVE -> items.filter { it.card.isVigente }
            FavoritesFilter.ENDED -> items.filterNot { it.card.isVigente }
        }
    val liveCount get() = items.count { it.card.isVigente }
    val endedCount get() = items.size - liveCount
    val savedCount get() = items.count { it.favorite }
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: ExploreRepository,
    private val locationStore: ClientLocationDataStore,
    private val favoritesSync: FavoritesSync,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var job: Job? = null
    private val inFlight = mutableSetOf<Long>()

    fun load() {
        if (job?.isActive == true) return
        _state.update { it.copy(loading = it.items.isEmpty(), error = null) }
        job = viewModelScope.launch {
            val location = locationStore.location.first()
            val all = mutableListOf<ExploreOfferUi>()
            var page = 0
            while (true) {
                val result = repository.getFavorites(location?.latitude, location?.longitude, page)
                val data = result.getOrElse { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "No pudimos cargar tus favoritos.") }
                    return@launch
                }
                all += data.items.map(ExploreOfferMapper::toUi)
                if (!data.hasMore) break
                page++
            }
            _state.update { it.copy(loading = false, items = all, error = null) }
        }
    }

    fun setFilter(filter: FavoritesFilter) = _state.update { it.copy(filter = filter) }

    /** La tarjeta queda en la lista con el corazón vacío: se puede volver a marcar. Desaparece al recargar. */
    fun toggleFavorite(offerId: Long) {
        val offer = _state.value.items.firstOrNull { it.id == offerId } ?: return
        if (!inFlight.add(offerId)) return

        val target = !offer.favorite
        setLocal(offerId, target)
        favoritesSync.set(offerId, target)

        viewModelScope.launch {
            repository.setFavorite(offerId, target).onFailure { e ->
                setLocal(offerId, !target)
                favoritesSync.set(offerId, !target)
                _state.update { it.copy(message = e.message) }
            }
            inFlight.remove(offerId)
        }
    }

    private fun setLocal(offerId: Long, favorite: Boolean) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == offerId) it.copy(favorite = favorite) else it })
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }
}