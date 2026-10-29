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
    val editingComment: ForumCommentDto? = null,
    val isSavingComment: Boolean = false,
    val editCommentError: String? = null,
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

    // El primer ON_RESUME coincide con la carga de init; se ignora para no pedir dos veces.
    private var skipNextResume = true

    private val pendingUpvotes = mutableSetOf<String>()

    init {
        loadData()
    }

    /** Recarga sin spinner al volver a la pantalla (por ejemplo, después de editar la publicación). */
    fun refreshOnResume() {
        if (skipNextResume) {
            skipNextResume = false
            return
        }
        loadData(showLoading = false)
    }

    fun loadData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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

    fun startEditComment(comment: ForumCommentDto) {
        _uiState.update { it.copy(editingComment = comment, editCommentError = null) }
    }

    fun cancelEditComment() {
        if (_uiState.value.isSavingComment) return
        _uiState.update { it.copy(editingComment = null, editCommentError = null) }
    }

    fun saveCommentEdit(texto: String) {
        val comment = _uiState.value.editingComment ?: return
        if (texto.isBlank() || _uiState.value.isSavingComment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingComment = true, editCommentError = null) }
            forumRepository.updateComment(comment.id, texto)
                .onSuccess { updated ->
                    _uiState.update { current ->
                        current.copy(
                            comments = current.comments.map {
                                if (it.id == updated.id) it.copy(texto = updated.texto) else it
                            },
                            editingComment = null,
                            isSavingComment = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingComment = false,
                            editCommentError = error.message ?: "No pudimos guardar el comentario",
                        )
                    }
                }
        }
    }

    fun togglePostUpvote() {
        val post = _uiState.value.post ?: return
        val key = "post-${post.id}"
        if (post.esUsuarioActual || key in pendingUpvotes) return
        pendingUpvotes += key

        val wasUpvoted = post.upvotedByMe
        _uiState.update {
            it.copy(post = post.copy(
                upvotedByMe = !wasUpvoted,
                upvotesCount = post.upvotesCount + if (wasUpvoted) -1 else 1,
            ))
        }

        viewModelScope.launch {
            forumRepository.togglePostUpvote(post.id, wasUpvoted)
                .onSuccess { r ->
                    _uiState.update {
                        it.copy(post = it.post?.copy(upvotesCount = r.upvotesCount, upvotedByMe = r.upvotedByMe))
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            post = it.post?.copy(upvotesCount = post.upvotesCount, upvotedByMe = wasUpvoted),
                            errorMessage = e.message ?: "No pudimos registrar tu voto",
                        )
                    }
                }
            pendingUpvotes -= key
        }
    }

    fun toggleCommentUpvote(commentId: Long) {
        val comment = _uiState.value.comments.find { it.id == commentId } ?: return
        val key = "comment-$commentId"
        if (comment.esUsuarioActual || key in pendingUpvotes) return
        pendingUpvotes += key

        val wasUpvoted = comment.upvotedByMe
        updateComment(commentId) {
            it.copy(
                upvotedByMe = !wasUpvoted,
                upvotesCount = it.upvotesCount + if (wasUpvoted) -1 else 1,
            )
        }

        viewModelScope.launch {
            forumRepository.toggleCommentUpvote(commentId, wasUpvoted)
                .onSuccess { r ->
                    updateComment(commentId) { it.copy(upvotesCount = r.upvotesCount, upvotedByMe = r.upvotedByMe) }
                }
                .onFailure { e ->
                    updateComment(commentId) { it.copy(upvotesCount = comment.upvotesCount, upvotedByMe = wasUpvoted) }
                    _uiState.update { it.copy(errorMessage = e.message ?: "No pudimos registrar tu voto") }
                }
            pendingUpvotes -= key
        }
    }

    private fun updateComment(id: Long, transform: (ForumCommentDto) -> ForumCommentDto) {
        _uiState.update { state ->
            state.copy(comments = state.comments.map { if (it.id == id) transform(it) else it })
        }
    }
}
