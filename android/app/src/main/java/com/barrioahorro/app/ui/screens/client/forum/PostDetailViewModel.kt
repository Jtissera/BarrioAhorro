package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.forum.CreateCommentDto
import com.barrioahorro.app.data.forum.ForumCommentDto
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostDetailUiState(
    val post: ForumPostDto? = null,
    val comments: List<ForumCommentDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSendingComment: Boolean = false,
    val replyingTo: ForumCommentDto? = null,
    val commentText: String = "",
    val errorMessage: String? = null,
    val focusedCommentStack: List<Long> = emptyList(),
) {
    val isSendEnabled: Boolean
        get() = commentText.trim().isNotEmpty() && !isSendingComment

    val focusedCommentId: Long?
        get() = focusedCommentStack.lastOrNull()

    val focusedComment: ForumCommentDto?
        get() = comments.find { it.id == focusedCommentId }
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val forumRepository: ForumRepository,
) : ViewModel() {

    val postId: Long = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val postResult = forumRepository.getPostById(postId)
            val commentsResult = forumRepository.getComments(postId)

            if (postResult.isSuccess && commentsResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        post = postResult.getOrNull(),
                        comments = commentsResult.getOrNull().orEmpty(),
                        isLoading = false,
                    )
                }
            } else {
                val error = postResult.exceptionOrNull() ?: commentsResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error?.message ?: "Error al cargar la publicación",
                    )
                }
            }
        }
    }

    fun onCommentTextChanged(newText: String) {
        if (newText.length <= 500) {
            _uiState.update { it.copy(commentText = newText, errorMessage = null) }
        }
    }

    fun onReplyToComment(comment: ForumCommentDto) {
        _uiState.update { it.copy(replyingTo = comment) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun focusComment(commentId: Long) {
        _uiState.update { current ->
            current.copy(
                focusedCommentStack = current.focusedCommentStack + commentId,
                replyingTo = null,
            )
        }
    }

    fun navigateUpComment(): Boolean {
        if (_uiState.value.focusedCommentStack.isNotEmpty()) {
            _uiState.update { current ->
                current.copy(
                    focusedCommentStack = current.focusedCommentStack.dropLast(1),
                    replyingTo = null,
                )
            }
            return true
        }
        return false
    }

    fun sendComment() {
        val state = _uiState.value
        val text = state.commentText.trim()
        if (text.isEmpty() || state.isSendingComment) return

        val parentId = state.replyingTo?.id ?: state.focusedCommentId

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true, errorMessage = null) }

            val dto = CreateCommentDto(
                texto = text,
                comentarioPadreId = parentId,
            )

            forumRepository.addComment(postId, dto)
                .onSuccess { newComment ->
                    _uiState.update { current ->
                        val updatedComments = current.comments.map { c ->
                            if (c.id == newComment.comentarioPadreId) {
                                c.copy(respuestasCount = c.respuestasCount + 1)
                            } else {
                                c
                            }
                        } + newComment

                        val updatedPost = current.post?.copy(
                            comentariosCount = current.post.comentariosCount + 1,
                        )
                        current.copy(
                            post = updatedPost,
                            comments = updatedComments,
                            commentText = "",
                            replyingTo = null,
                            isSendingComment = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSendingComment = false,
                            errorMessage = error.message ?: "No se pudo enviar el comentario",
                        )
                    }
                }
        }
    }
}
