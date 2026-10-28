package com.barrioahorro.app.ui.screens.client.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.local.datastore.ClientLocation
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import com.barrioahorro.app.data.profile.BusinessProfileRepository
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.screens.business.OpenStatus
import com.barrioahorro.app.ui.screens.business.ProfileReviewUi
import com.barrioahorro.app.ui.screens.business.ProfileTab
import com.barrioahorro.app.ui.screens.business.ReviewsSummaryUi
import com.barrioahorro.app.ui.screens.business.computeOpenStatus
import com.barrioahorro.app.ui.screens.business.isOpenToday
import com.barrioahorro.app.ui.screens.business.resolvePhotoUrl
import com.barrioahorro.app.ui.screens.business.toScheduleUi
import com.barrioahorro.app.ui.screens.business.toUi
import com.barrioahorro.app.ui.screens.client.explore.ExploreOfferMapper
import com.barrioahorro.app.ui.screens.client.explore.ExploreOfferUi
import com.barrioahorro.app.ui.screens.client.explore.FavoritesSync
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject

private const val REVIEWS_PAGE_SIZE = 10

data class ClientBusinessProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val businessName: String = "",
    val categoryName: String = "",
    val address: String = "",
    val description: String = "",
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceText: String = "",
    val schedule: List<DayScheduleUi> = emptyList(),
    val openStatus: OpenStatus = OpenStatus(isOpen = false, text = ""),
    val isOpenToday: Boolean = false,
    val todayDiaSemana: Int = 0,
    val selectedTab: ProfileTab = ProfileTab.OFFERS,
    // Ofertas. offers == null significa "todavía no cargó".
    val offers: List<ExploreOfferUi>? = null,
    val offersError: String? = null,
    // Reseñas. reviewsSummary == null significa "todavía no cargó".
    val reviewsSummary: ReviewsSummaryUi? = null,
    val myReview: ProfileReviewUi? = null,
    val reviews: List<ProfileReviewUi> = emptyList(),
    val hasMoreReviews: Boolean = false,
    val isLoadingMoreReviews: Boolean = false,
    val reviewsError: String? = null,
    val showReviewForm: Boolean = false,
    val isSavingReview: Boolean = false,
    val reviewFormError: String? = null,
    val isDeletingReview: Boolean = false,
)

@HiltViewModel
class ClientBusinessProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: BusinessProfileRepository,
    private val exploreRepository: ExploreRepository,
    private val locationStore: ClientLocationDataStore,
    private val favoritesSync: FavoritesSync,
) : ViewModel() {

    private val businessId: Long = checkNotNull(savedStateHandle.get<Long>("businessId"))

    private val _state = MutableStateFlow(ClientBusinessProfileUiState())
    val state: StateFlow<ClientBusinessProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var nextReviewsPage = 1
    private val favoritesInFlight = mutableSetOf<Long>()

    init {
        load()
        // Favoritos cambiados desde otra pantalla se reflejan en el corazón de estas tarjetas.
        viewModelScope.launch {
            favoritesSync.changes.collect { changes ->
                _state.update { s ->
                    s.copy(offers = s.offers?.map { o -> changes[o.id]?.let { o.copy(favorite = it) } ?: o })
                }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    // ---- Carga ----------------------------------------------------------------------

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val location = locationStore.location.first()

            val profile = profileRepository.getProfile(businessId, location?.latitude, location?.longitude)
                .getOrElse { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "No pudimos cargar el comercio") }
                    return@launch
                }

            val schedule = profile.schedule.toScheduleUi()
            val now = LocalDateTime.now()
            val today = now.dayOfWeek.value
            val distance = ExploreOfferMapper.formatDistance(profile.distanceMeters)

            _state.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    businessName = profile.name ?: "Comercio",
                    categoryName = profile.category.orEmpty(),
                    address = profile.address.orEmpty(),
                    description = profile.description.orEmpty(),
                    photoUrl = profile.photoUrl?.let { url -> resolvePhotoUrl(BASE_URL, url) },
                    latitude = profile.latitude,
                    longitude = profile.longitude,
                    distanceText = if (distance.isNotBlank()) "$distance de tu ubicación" else "",
                    schedule = schedule,
                    openStatus = computeOpenStatus(schedule, now),
                    isOpenToday = isOpenToday(schedule, today),
                    todayDiaSemana = today,
                )
            }

            // Cada pestaña maneja su propio error: si una falla, las otras siguen funcionando.
            coroutineScope {
                launch { loadOffers(location) }
                launch { loadReviews() }
            }
        }
    }

    fun retryOffers() {
        viewModelScope.launch {
            _state.update { it.copy(offersError = null) }
            loadOffers(locationStore.location.first())
        }
    }

    fun retryReviews() {
        viewModelScope.launch {
            _state.update { it.copy(reviewsError = null) }
            loadReviews()
        }
    }

    private suspend fun loadOffers(location: ClientLocation?) {
        profileRepository.getOffers(businessId, location?.latitude, location?.longitude)
            .onSuccess { list ->
                _state.update { it.copy(offers = list.map(ExploreOfferMapper::toUi), offersError = null) }
            }
            .onFailure { e ->
                _state.update { it.copy(offersError = e.message ?: "No pudimos cargar las ofertas") }
            }
    }

    private suspend fun loadReviews() {
        profileRepository.getReviews(businessId, page = 0, size = REVIEWS_PAGE_SIZE)
            .onSuccess { page ->
                nextReviewsPage = 1
                val now = OffsetDateTime.now()
                _state.update {
                    it.copy(
                        reviewsSummary = page.summary.toUi(),
                        myReview = page.mine?.toUi(now),
                        reviews = page.items.map { item -> item.toUi(now) },
                        hasMoreReviews = page.hasNext,
                        reviewsError = null,
                    )
                }
            }
            .onFailure { e ->
                _state.update { it.copy(reviewsError = e.message ?: "No pudimos cargar las reseñas") }
            }
    }

    fun loadMoreReviews() {
        val current = _state.value
        if (!current.hasMoreReviews || current.isLoadingMoreReviews) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreReviews = true, reviewsError = null) }
            profileRepository.getReviews(businessId, page = nextReviewsPage, size = REVIEWS_PAGE_SIZE)
                .onSuccess { page ->
                    nextReviewsPage += 1
                    val now = OffsetDateTime.now()
                    _state.update {
                        it.copy(
                            isLoadingMoreReviews = false,
                            reviewsSummary = page.summary.toUi(),
                            reviews = (it.reviews + page.items.map { item -> item.toUi(now) }).distinctBy { r -> r.id },
                            hasMoreReviews = page.hasNext,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMoreReviews = false, reviewsError = e.message) }
                }
        }
    }

    // ---- Reseña propia --------------------------------------------------------------

    fun openReviewForm() {
        _state.update { it.copy(showReviewForm = true, reviewFormError = null) }
    }

    fun closeReviewForm() {
        if (_state.value.isSavingReview) return
        _state.update { it.copy(showReviewForm = false, reviewFormError = null) }
    }

    fun saveReview(rating: Int, comment: String) {
        if (_state.value.isSavingReview) return
        val isEdit = _state.value.myReview != null
        _state.update { it.copy(isSavingReview = true, reviewFormError = null) }

        viewModelScope.launch {
            profileRepository.saveReview(businessId, rating, comment.trim())
                .onSuccess { _ ->
                    _state.update {
                        it.copy(
                            isSavingReview = false,
                            showReviewForm = false,
                            message = if (isEdit) "Actualizamos tu reseña" else "¡Gracias por tu reseña!",
                        )
                    }
                    loadReviews()
                }
                .onFailure { e ->
                    _state.update { it.copy(isSavingReview = false, reviewFormError = e.message) }
                }
        }
    }

    fun deleteReview() {
        if (_state.value.isDeletingReview) return
        _state.update { it.copy(isDeletingReview = true) }

        viewModelScope.launch {
            profileRepository.deleteReview(businessId)
                .onSuccess { _ ->
                    _state.update { it.copy(isDeletingReview = false, message = "Eliminaste tu reseña") }
                    loadReviews()
                }
                .onFailure { e ->
                    _state.update { it.copy(isDeletingReview = false, message = e.message) }
                }
        }
    }

    // ---- Favoritos ------------------------------------------------------------------

    fun toggleFavorite(offerId: Long) {
        val offer = _state.value.offers?.firstOrNull { it.id == offerId } ?: return
        if (!favoritesInFlight.add(offerId)) return

        val target = !offer.favorite
        setFavoriteLocally(offerId, target)
        favoritesSync.set(offerId, target)

        viewModelScope.launch {
            exploreRepository.setFavorite(offerId, target).onFailure { e ->
                setFavoriteLocally(offerId, !target)
                favoritesSync.set(offerId, !target)
                _state.update { it.copy(message = e.message) }
            }
            favoritesInFlight.remove(offerId)
        }
    }

    private fun setFavoriteLocally(offerId: Long, favorite: Boolean) {
        _state.update { s ->
            s.copy(offers = s.offers?.map { if (it.id == offerId) it.copy(favorite = favorite) else it })
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }
}
