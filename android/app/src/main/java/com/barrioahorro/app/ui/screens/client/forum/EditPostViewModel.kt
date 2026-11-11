package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import com.barrioahorro.app.data.forum.UpdateForumPostDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPostUiState(
    val post: ForumPostDto? = null,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val text: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
) {
    val isSaveEnabled: Boolean
        get() = post != null && text.trim().isNotEmpty() && !isSaving
}

@HiltViewModel
class EditPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val forumRepository: ForumRepository,
) : ViewModel() {

    private val postId: Long = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    init {
        loadPost()
    }

    fun loadPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            forumRepository.getPostById(postId)
                .onSuccess { post ->
                    _uiState.update {
                        it.copy(
                            post = post,
                            isLoading = false,
                            text = post.texto,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, loadError = error.message ?: "No pudimos cargar la publicación")
                    }
                }
        }
    }

    fun onTextChanged(newText: String) {
        if (newText.length <= FORUM_POST_MAX_LENGTH) {
            _uiState.update { it.copy(text = newText, errorMessage = null) }
        }
    }

    fun save() {
        val state = _uiState.value
        val post = state.post ?: return
        val text = state.text.trim()
        if (text.isEmpty() || state.isSaving) return

        // Se reenvía el punto original; precio y dirección quedan en null (se borran).
        val dto = UpdateForumPostDto(
            texto = text,
            latitud = post.latitud,
            longitud = post.longitud,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            forumRepository.updatePost(postId, dto)
                .onSuccess { updated ->
                    _uiState.update { it.copy(post = updated, isSaving = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "No pudimos guardar los cambios")
                    }
                }
        }
    }
}
