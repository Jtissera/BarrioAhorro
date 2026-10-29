package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForumFeedUiState(
    val posts: List<ForumPostDto> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val upvoteError: String? = null,
)

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumFeedUiState())
    val uiState: StateFlow<ForumFeedUiState> = _uiState.asStateFlow()
    private val pendingUpvotes = mutableSetOf<Long>()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            forumRepository.getFeed()
                .onSuccess { list ->
                    _uiState.update { it.copy(posts = list, isLoading = false, isRefreshing = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "No pudimos cargar las publicaciones",
                        )
                    }
                }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            forumRepository.getFeed()
                .onSuccess { list ->
                    _uiState.update { it.copy(posts = list, isRefreshing = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "No pudimos actualizar el feed",
                        )
                    }
                }
        }
    }

    fun toggleUpvote(postId: Long) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        if (post.esUsuarioActual || postId in pendingUpvotes) return
        pendingUpvotes += postId

        val wasUpvoted = post.upvotedByMe
        updatePost(postId) {
            it.copy(
                upvotedByMe = !wasUpvoted,
                upvotesCount = it.upvotesCount + if (wasUpvoted) -1 else 1,
            )
        }

        viewModelScope.launch {
            forumRepository.togglePostUpvote(postId, wasUpvoted)
                .onSuccess { result ->
                    updatePost(postId) {
                        it.copy(upvotesCount = result.upvotesCount, upvotedByMe = result.upvotedByMe)
                    }
                }
                .onFailure { error ->
                    updatePost(postId) {
                        it.copy(upvotesCount = post.upvotesCount, upvotedByMe = wasUpvoted)
                    }
                    _uiState.update { it.copy(upvoteError = error.message ?: "No pudimos registrar tu voto") }
                }
            pendingUpvotes -= postId
        }
    }

    fun onUpvoteErrorShown() {
        _uiState.update { it.copy(upvoteError = null) }
    }

    private fun updatePost(id: Long, transform: (ForumPostDto) -> ForumPostDto) {
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == id) transform(it) else it })
        }
    }

}
