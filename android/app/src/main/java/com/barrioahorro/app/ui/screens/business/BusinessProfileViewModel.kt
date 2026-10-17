package com.barrioahorro.app.ui.screens.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.screens.offers.MyOfferItemUi
import com.barrioahorro.app.ui.screens.offers.OfferUiMapper
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject

private const val REVIEWS_PAGE_SIZE = 10

enum class ProfileTab { OFFERS, REVIEWS, INFO }

data class BusinessProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val businessName: String = "",
    val categoryName: String = "",
    val direccion: String = "",
    val descripcion: String = "",
    val coverUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val schedule: List<DayScheduleUi> = emptyList(),
    val openStatus: OpenStatus = OpenStatus(isOpen = false, text = ""),
    val isOpenToday: Boolean = false,
    val todayDiaSemana: Int = 0,
    val selectedTab: ProfileTab = ProfileTab.OFFERS,
    // Offers tab. activeCount == null means "not loaded yet".
    val activeCount: Int? = null,
    val offers: List<MyOfferItemUi> = emptyList(),
    val offersError: String? = null,
    // Reviews tab. reviewsSummary == null means "not loaded yet".
    val reviewsSummary: ReviewsSummaryUi? = null,
    val reviews: List<ProfileReviewUi> = emptyList(),
    val hasMoreReviews: Boolean = false,
    val isLoadingMoreReviews: Boolean = false,
    val reviewsError: String? = null,
)

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState

    private var loaded = false
    private var offersLoaded = false
    private var reviewsLoaded = false
    private var nextReviewsPage = 0

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

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

                val schedule = business.horarios.toScheduleUi()
                val now = LocalDateTime.now()
                val today = now.dayOfWeek.value
                val businessName = business.businessName.orEmpty()
                val categoryName = business.categoryName.orEmpty()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        businessName = businessName,
                        categoryName = categoryName,
                        direccion = business.direccion.orEmpty(),
                        descripcion = business.descripcion.orEmpty(),
                        coverUrl = business.fotos.toPhotoUi(BASE_URL).firstOrNull()?.url,
                        latitud = business.latitud,
                        longitud = business.longitud,
                        schedule = schedule,
                        openStatus = computeOpenStatus(schedule, now),
                        isOpenToday = isOpenToday(schedule, today),
                        todayDiaSemana = today,
                    )
                }

                // Cada pestaña maneja su propio error: si una falla, las otras siguen funcionando.
                coroutineScope {
                    launch { loadOffers(businessName, categoryName) }
                    launch { loadReviews() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!loaded) _uiState.update { it.copy(isLoading = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun loadMoreReviews() {
        val current = _uiState.value
        if (!current.hasMoreReviews || current.isLoadingMoreReviews) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreReviews = true, reviewsError = null) }
            try {
                val response = businessApiService.getMyReviews(page = nextReviewsPage, size = REVIEWS_PAGE_SIZE)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.update {
                        it.copy(isLoadingMoreReviews = false, reviewsError = "No pudimos cargar más reseñas")
                    }
                    return@launch
                }
                nextReviewsPage += 1
                val now = OffsetDateTime.now()
                _uiState.update {
                    it.copy(
                        isLoadingMoreReviews = false,
                        reviewsSummary = body.summary.toUi(),
                        reviews = it.reviews + body.items.map { item -> item.toUi(now) },
                        hasMoreReviews = body.hasNext,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMoreReviews = false, reviewsError = "Sin conexión, probá de nuevo") }
            }
        }
    }

    private suspend fun loadOffers(businessName: String, categoryName: String) {
        try {
            val response = businessApiService.getMyActiveOffers()
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                if (!offersLoaded) _uiState.update { it.copy(offersError = "No pudimos cargar tus ofertas") }
                return
            }
            offersLoaded = true
            _uiState.update {
                it.copy(
                    activeCount = body.activeCount,
                    offers = body.items.map { item ->
                        OfferUiMapper.toUi(item.offer, businessName, categoryName, item.confirmationsCount)
                    },
                    offersError = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!offersLoaded) _uiState.update { it.copy(offersError = "Sin conexión, probá de nuevo") }
        }
    }

    private suspend fun loadReviews() {
        try {
            val response = businessApiService.getMyReviews(page = 0, size = REVIEWS_PAGE_SIZE)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                if (!reviewsLoaded) _uiState.update { it.copy(reviewsError = "No pudimos cargar tus reseñas") }
                return
            }
            reviewsLoaded = true
            nextReviewsPage = 1
            val now = OffsetDateTime.now()
            _uiState.update {
                it.copy(
                    reviewsSummary = body.summary.toUi(),
                    reviews = body.items.map { item -> item.toUi(now) },
                    hasMoreReviews = body.hasNext,
                    reviewsError = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!reviewsLoaded) _uiState.update { it.copy(reviewsError = "Sin conexión, probá de nuevo") }
        }
    }
}