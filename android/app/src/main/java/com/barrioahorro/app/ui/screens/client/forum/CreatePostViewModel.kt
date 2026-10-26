package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreBusinessDto
import com.barrioahorro.app.data.explore.ExploreQuery
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.forum.CreateForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePostUiState(
    val businesses: List<ExploreBusinessDto> = emptyList(),
    val filteredBusinesses: List<ExploreBusinessDto> = emptyList(),
    val isLoadingBusinesses: Boolean = false,
    val selectedBusiness: ExploreBusinessDto? = null,
    val text: String = "",
    val price: String = "",
    val locationDescription: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isBusinessSelectorOpen: Boolean = false,
    val businessSearchQuery: String = "",
) {
    val isSubmitEnabled: Boolean
        get() = selectedBusiness != null && text.trim().isNotEmpty() && !isSubmitting
}

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val exploreRepository: ExploreRepository,
    private val locationDataStore: ClientLocationDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    init {
        loadBusinesses()
    }

    fun loadBusinesses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBusinesses = true) }
            val clientLoc = locationDataStore.location.firstOrNull()
            val lat = clientLoc?.latitude ?: -34.6037
            val lng = clientLoc?.longitude ?: -58.3816

            val query = ExploreQuery(
                latitude = lat,
                longitude = lng,
                radiusKm = 20.0,
                categoryIds = emptyList(),
                types = emptyList(),
                text = null,
                order = "DISTANCE",
            )

            exploreRepository.getBusinesses(query)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            businesses = list,
                            filteredBusinesses = list,
                            isLoadingBusinesses = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingBusinesses = false) }
                }
        }
    }

    fun onBusinessSearchQueryChanged(q: String) {
        _uiState.update { state ->
            val filtered = if (q.isBlank()) {
                state.businesses
            } else {
                state.businesses.filter {
                    (it.name ?: "").contains(q, ignoreCase = true) ||
                        (it.category ?: "").contains(q, ignoreCase = true)
                }
            }
            state.copy(businessSearchQuery = q, filteredBusinesses = filtered)
        }
    }

    fun openBusinessSelector() {
        _uiState.update { it.copy(isBusinessSelectorOpen = true, businessSearchQuery = "", filteredBusinesses = it.businesses) }
    }

    fun closeBusinessSelector() {
        _uiState.update { it.copy(isBusinessSelectorOpen = false) }
    }

    fun onBusinessSelected(business: ExploreBusinessDto) {
        _uiState.update {
            it.copy(
                selectedBusiness = business,
                isBusinessSelectorOpen = false,
                errorMessage = null,
            )
        }
    }

    fun onTextChanged(newText: String) {
        if (newText.length <= FORUM_POST_MAX_LENGTH) {
            _uiState.update { it.copy(text = newText, errorMessage = null) }
        }
    }

    fun onPriceChanged(newPrice: String) {
        _uiState.update { it.copy(price = newPrice) }
    }

    fun onLocationDescriptionChanged(newLocation: String) {
        _uiState.update { it.copy(locationDescription = newLocation) }
    }

    fun submitPost() {
        val state = _uiState.value
        val business = state.selectedBusiness ?: return
        val text = state.text.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            // Extraer valor numérico del precio si fue ingresado
            val numericPrice = parseForumPrice(state.price)

            val clientLoc = locationDataStore.location.firstOrNull()

            val dto = CreateForumPostDto(
                comercioId = business.id,
                texto = text,
                precioObservado = numericPrice,
                direccion = state.locationDescription.trim().takeIf { it.isNotEmpty() },
                latitud = clientLoc?.latitude ?: business.latitude,
                longitud = clientLoc?.longitude ?: business.longitude,
            )

            forumRepository.createPost(dto)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "No pudimos crear la publicación",
                        )
                    }
                }
        }
    }
}
