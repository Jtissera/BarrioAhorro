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
    val price: String = "",
    val locationDescription: String = "",
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
                            price = formatForumPriceInput(post.precioObservado),
                            locationDescription = post.direccion.orEmpty(),
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

    fun onPriceChanged(newPrice: String) {
        _uiState.update { it.copy(price = sanitizePriceInput(newPrice), errorMessage = null) }
    }

    fun onLocationDescriptionChanged(newLocation: String) {
        _uiState.update { it.copy(locationDescription = newLocation, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        val post = state.post ?: return
        val text = state.text.trim()
        if (text.isEmpty() || state.isSaving) return

        val price = parseForumPrice(state.price)
        if (state.price.isNotBlank() && price == null) {
            _uiState.update { it.copy(errorMessage = "Ingresá un precio mayor a cero o dejalo vacío.") }
            return
        }

        // Se manda el formulario completo. Si la ubicación no cambió se reenvía el punto
        // original (también sirve para publicaciones viejas sin dirección escrita);
        // si cambió, el backend la geocodifica; si se borró, se limpia.
        val direccion = state.locationDescription.trim()
        val unchangedLocation = direccion == post.direccion.orEmpty().trim()
        val dto = UpdateForumPostDto(
            texto = text,
            precioObservado = price,
            direccion = direccion.ifEmpty { null },
            latitud = if (unchangedLocation) post.latitud else null,
            longitud = if (unchangedLocation) post.longitud else null,
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
