package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import com.barrioahorro.app.data.forum.MyForumCommentDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPostsUiState(
    val puntos: Int = 0,
    val posts: List<ForumPostDto> = emptyList(),
    val comments: List<MyForumCommentDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = _uiState.asStateFlow()

    init {
        loadMyActivity()
    }

    fun loadMyActivity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            forumRepository.getMyActivity()
                .onSuccess { activity ->
                    _uiState.update {
                        it.copy(
                            puntos = activity.puntos,
                            posts = activity.publicaciones,
                            comments = activity.comentarios,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No pudimos cargar tus publicaciones",
                        )
                    }
                }
        }
    }
}
