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
    val editingComment: MyForumCommentDto? = null,
    val isSavingComment: Boolean = false,
    val editCommentError: String? = null,
)

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = _uiState.asStateFlow()

    // El primer ON_RESUME coincide con la carga de init; se ignora para no pedir dos veces.
    private var skipNextResume = true

    init {
        loadMyActivity()
    }

    /** Recarga al volver a la pantalla (por ejemplo, después de editar una publicación). */
    fun refreshOnResume() {
        if (skipNextResume) {
            skipNextResume = false
            return
        }
        loadMyActivity(showLoading = false)
    }

    fun loadMyActivity(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            forumRepository.getMyActivity()
                .onSuccess { activity ->
                    _uiState.update {
                        it.copy(
                            puntos = activity.puntos,
                            posts = activity.publicaciones,
                            comments = activity.comentarios,
                            isLoading = false,
                            errorMessage = null,
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

    fun startEditComment(comment: MyForumCommentDto) {
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
}
