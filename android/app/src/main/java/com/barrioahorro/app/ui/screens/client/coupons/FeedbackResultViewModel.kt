package com.barrioahorro.app.ui.screens.client.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackResultState(
    val redemption: RedemptionUi? = null,
    val pointsAwarded: Int = 0,
)

@HiltViewModel
class FeedbackResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExploreRepository,
) : ViewModel() {

    private val redemptionId: Long = checkNotNull(savedStateHandle.get<Long>("redemptionId"))

    private val _state = MutableStateFlow(FeedbackResultState())
    val state: StateFlow<FeedbackResultState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRedemption(redemptionId)
                .onSuccess { dto -> _state.update { it.copy(redemption = dto.toUi()) } }
        }
        viewModelScope.launch {
            repository.getFeedback(redemptionId)
                .onSuccess { f -> _state.update { it.copy(pointsAwarded = f.pointsAwarded) } }
        }
    }
}