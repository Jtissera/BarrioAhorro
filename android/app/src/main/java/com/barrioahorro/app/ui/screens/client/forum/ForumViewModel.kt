package com.barrioahorro.app.ui.screens.client.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.forum.ForumBusinessDto
import com.barrioahorro.app.data.forum.ForumFeedFilters
import com.barrioahorro.app.data.forum.ForumPostDto
import com.barrioahorro.app.data.forum.ForumRepository
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForumFeedUiState(
    val posts: List<ForumPostDto> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val upvoteError: String? = null,
    val filters: ForumFeedFilters = ForumFeedFilters(),
    val businesses: List<ForumBusinessDto> = emptyList(),
    val locationError: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val locationStore: ClientLocationDataStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumFeedUiState())
    val uiState: StateFlow<ForumFeedUiState> = _uiState.asStateFlow()
    private val pendingUpvotes = mutableSetOf<Long>()
    private var feedJob: Job? = null

    init {
        loadBusinesses()
        loadFeed()
    }

    // ---------- Carga ----------

    /** Al volver a la pantalla: spinner solo si todavía no hay nada que mostrar. */
    fun loadFeed() = fetchFeed(showSpinner = _uiState.value.posts.isEmpty())

    fun refreshFeed() {
        loadBusinesses()
        fetchFeed(showSpinner = false, refreshing = true)
    }

    private fun fetchFeed(showSpinner: Boolean, refreshing: Boolean = false) {
        feedJob?.cancel() // evita que una respuesta vieja pise a la de los filtros nuevos
        feedJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = showSpinner, isRefreshing = refreshing, errorMessage = null)
            }
            forumRepository.getFeed(_uiState.value.filters)
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

    private fun loadBusinesses() {
        viewModelScope.launch {
            forumRepository.getFeedBusinesses()
                .onSuccess { list -> _uiState.update { it.copy(businesses = list) } }
        }
    }



    // ---------- Filtros ----------

    fun onComercioSelected(id: Long?, nombre: String?) =
        applyFilters { it.copy(comercioId = id, comercioNombre = nombre) }

    /** Activa o quita "Cerca tuyo" usando la ubicación que el usuario ya guardó en la app. */
    fun onCercaToggled() {
        if (_uiState.value.filters.cercaActivo) {
            applyFilters { it.copy(lat = null, lng = null) }
            return
        }
        viewModelScope.launch {
            val saved = locationStore.location.first()
            if (saved == null) {
                _uiState.update {
                    it.copy(locationError = "Todavía no configuraste tu ubicación. Podés hacerlo desde tu perfil.")
                }
            } else {
                applyFilters { it.copy(lat = saved.latitude, lng = saved.longitude) }
            }
        }
    }

    fun onDiasSelected(dias: Int?) = applyFilters { it.copy(dias = dias) }

    fun clearFilters() = applyFilters { ForumFeedFilters() }

    fun onLocationErrorShown() {
        _uiState.update { it.copy(locationError = null) }
    }

    private fun applyFilters(transform: (ForumFeedFilters) -> ForumFeedFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
        fetchFeed(showSpinner = true)
    }

    fun onUpvoteErrorShown() {
        _uiState.update { it.copy(upvoteError = null) }
    }

    // ---------- Upvotes ----------

    fun toggleUpvote(postId: Long) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return

        if (post.esUsuarioActual || post.upvotedByMe || postId in pendingUpvotes) return
        pendingUpvotes += postId

        updatePost(postId) {
            it.copy(upvotedByMe = true, upvotesCount = it.upvotesCount + 1)
        }

        viewModelScope.launch {
            forumRepository.togglePostUpvote(postId, false)
                .onSuccess { result ->
                    updatePost(postId) {
                        it.copy(upvotesCount = result.upvotesCount, upvotedByMe = result.upvotedByMe)
                    }
                }
                .onFailure { error ->
                    updatePost(postId) {
                        it.copy(upvotesCount = post.upvotesCount, upvotedByMe = false)
                    }
                    _uiState.update { it.copy(upvoteError = error.message ?: "No pudimos registrar tu voto") }
                }
            pendingUpvotes -= postId
        }
    }
    private fun updatePost(id: Long, transform: (ForumPostDto) -> ForumPostDto) {
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == id) transform(it) else it })
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            forumRepository.deletePost(postId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            posts = current.posts.filterNot { it.id == postId },
                            feedbackMessage = "Publicación eliminada",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = error.message ?: "No se pudo eliminar la publicación"
                        )
                    }
                }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            forumRepository.deleteComment(commentId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            feedbackMessage = "Comentario eliminado"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = error.message ?: "No se pudo eliminar el comentario"
                        )
                    }
                }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun checkPendingFeedback() {
        savedStateHandle.get<String>("deleted_feedback")?.let { msg ->
            _uiState.update { it.copy(feedbackMessage = msg) }
            savedStateHandle.remove<String>("deleted_feedback")
        }
    }
}