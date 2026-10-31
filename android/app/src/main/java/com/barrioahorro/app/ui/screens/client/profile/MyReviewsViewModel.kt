package com.barrioahorro.app.ui.screens.client.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.profile.ClientProfileRepository
import com.barrioahorro.app.data.profile.MyReviewDto
import com.barrioahorro.app.ui.screens.business.formatRelativeDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import javax.inject.Inject

private const val PAGE_SIZE = 20

data class MyReviewItemUi(
    val id: Long,
    val businessId: Long,
    val businessName: String,
    val categoryName: String,
    val rating: Int,
    val comment: String,
    val relativeDate: String,
)

data class MyReviewsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<MyReviewItemUi> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val moreError: String? = null,
)

@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val repository: ClientProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyReviewsUiState())
    val state: StateFlow<MyReviewsUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var nextPage = 1

    /** Se llama al entrar y al volver (por si se editó o borró una reseña). Con datos ya cargados no muestra spinner. */
    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.items.isEmpty(), isLoadingMore = false, error = null) }
            repository.getMyReviews(page = 0, size = PAGE_SIZE)
                .onSuccess { page ->
                    nextPage = 1
                    val now = OffsetDateTime.now()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = page.items.map { dto -> dto.toUi(now) },
                            hasMore = page.hasNext,
                            moreError = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.items.isEmpty()) (e.message ?: "No pudimos cargar tus reseñas") else null,
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return

        _state.update { it.copy(isLoadingMore = true, moreError = null) }
        job = viewModelScope.launch {
            repository.getMyReviews(page = nextPage, size = PAGE_SIZE)
                .onSuccess { page ->
                    nextPage += 1
                    val now = OffsetDateTime.now()
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            items = (it.items + page.items.map { dto -> dto.toUi(now) }).distinctBy { r -> r.id },
                            hasMore = page.hasNext,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMore = false, moreError = e.message) }
                }
        }
    }

    private fun MyReviewDto.toUi(now: OffsetDateTime) = MyReviewItemUi(
        id = id,
        businessId = businessId,
        businessName = businessName ?: "Comercio",
        categoryName = category.orEmpty(),
        rating = rating.coerceIn(0, 5),
        comment = comment,
        relativeDate = formatRelativeDate(createdAt, now),
    )
}