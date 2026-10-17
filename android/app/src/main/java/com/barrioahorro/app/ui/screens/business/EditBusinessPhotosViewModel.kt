package com.barrioahorro.app.ui.screens.business

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.image.PhotoCompressor
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.dto.ReorderPhotosRequestDto
import com.barrioahorro.app.di.BASE_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject

data class EditBusinessPhotosUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val businessName: String = "",
    val draft: PhotoDraft = PhotoDraft.fromServer(emptyList()),
    val isSaving: Boolean = false,
    val error: String? = null,
    /** True once after a save that applied every change, until the screen shows the confirmation. */
    val showSavedMessage: Boolean = false,
)

/**
 * "Fotos del comercio": photo edits live in a local [PhotoDraft] and only reach the server on [save].
 */
@HiltViewModel
class EditBusinessPhotosViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val photoCompressor: PhotoCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBusinessPhotosUiState())
    val uiState: StateFlow<EditBusinessPhotosUiState> = _uiState

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                val response = businessApiService.getMyBusiness()
                val business = response.body()
                if (!response.isSuccessful || business == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = "No pudimos cargar tus fotos") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        businessName = business.businessName.orEmpty(),
                        draft = PhotoDraft.fromServer(business.fotos.toPhotoUi(BASE_URL)),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun addPhoto(uri: Uri) = editDraft { it.addPhoto(uri.toString()) }

    fun replacePhoto(key: String, uri: Uri) = editDraft { it.replacePhoto(key, uri.toString()) }

    fun removePhoto(key: String) = editDraft { it.removePhoto(key) }

    fun setAsCover(key: String) = editDraft { it.setAsCover(key) }

    fun onSavedMessageShown() {
        _uiState.update { it.copy(showSavedMessage = false) }
    }

    private fun editDraft(change: (PhotoDraft) -> PhotoDraft) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(draft = change(it.draft), error = null) }
    }

    /** Applies the pending operations in order, reloads the server photos and keeps whatever failed as pending. */
    fun save(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        if (state.isSaving || !state.draft.hasChanges) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, showSavedMessage = false) }
            val draft = state.draft
            val uploadedIds = mutableMapOf<String, Long>()
            var failure: String? = null

            for (operation in draft.pendingOperations()) {
                // The final order needs every photo on the server, so it waits for a clean run.
                if (operation is PhotoOperation.Reorder && failure != null) continue
                val error = try {
                    apply(operation, draft, uploadedIds)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "Revisá tu conexión."
                }
                if (failure == null) failure = error
            }

            val reloaded = try {
                val response = businessApiService.getMyBusiness()
                response.body()?.takeIf { response.isSuccessful }?.fotos?.toPhotoUi(BASE_URL)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            if (reloaded == null) {
                // Without the server state the draft can't tell what applied, so start over from the server.
                _uiState.update {
                    it.copy(isSaving = false, loadError = "No pudimos confirmar qué fotos se guardaron. Volvé a cargarlas.")
                }
                return@launch
            }

            val rebased = draft.rebase(reloaded, uploadedIds)
            val savedEverything = failure == null && !rebased.hasChanges
            _uiState.update {
                if (savedEverything) {
                    it.copy(isSaving = false, draft = rebased, showSavedMessage = true)
                } else {
                    it.copy(
                        isSaving = false,
                        draft = rebased,
                        error = "No pudimos guardar todos los cambios. ${failure ?: "Probá de nuevo."}",
                    )
                }
            }
            if (savedEverything) onSaved()
        }
    }

    /** Runs one operation; returns null when it applied, or a short reason when it did not. */
    private suspend fun apply(
        operation: PhotoOperation,
        draft: PhotoDraft,
        uploadedIds: MutableMap<String, Long>,
    ): String? = when (operation) {
        is PhotoOperation.Delete -> failureOf(businessApiService.deletePhoto(operation.photoId))

        is PhotoOperation.Replace -> {
            val part = photoPart(operation.uri)
            if (part == null) {
                UNREADABLE_IMAGE
            } else {
                val response = businessApiService.replacePhoto(operation.photoId, part)
                response.body()?.takeIf { response.isSuccessful }?.let { uploadedIds[operation.key] = it.id }
                failureOf(response)
            }
        }

        is PhotoOperation.Add -> {
            val part = photoPart(operation.uri)
            if (part == null) {
                UNREADABLE_IMAGE
            } else {
                val response = businessApiService.addPhoto(part)
                response.body()?.takeIf { response.isSuccessful }?.let { uploadedIds[operation.key] = it.id }
                failureOf(response)
            }
        }

        is PhotoOperation.Reorder -> {
            val serverIdByKey = draft.photos.associate { it.key to it.serverId }
            val ids = operation.keys.map { key -> uploadedIds[key] ?: serverIdByKey[key] }
            if (ids.any { it == null }) {
                "Probá de nuevo."
            } else {
                failureOf(businessApiService.reorderPhotos(ReorderPhotosRequestDto(ids.filterNotNull())))
            }
        }
    }

    private fun failureOf(response: Response<*>): String? = when {
        response.isSuccessful -> null
        response.code() == 409 -> "Podés tener hasta $MAX_BUSINESS_PHOTOS fotos."
        response.code() == 413 -> "Una de las fotos es demasiado grande."
        else -> "Probá de nuevo."
    }

    private suspend fun photoPart(uri: String): MultipartBody.Part? {
        val bytes = photoCompressor.compress(Uri.parse(uri)) ?: return null
        return MultipartBody.Part.createFormData("file", "foto.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
    }

    private companion object {
        const val UNREADABLE_IMAGE = "No pudimos leer una de las imágenes."
    }
}
