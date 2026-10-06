package com.barrioahorro.app.ui.screens.business

import android.annotation.SuppressLint
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.local.image.PhotoCompressor
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.dto.ReorderPhotosRequestDto
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.ui.screens.onboarding.CategoryUi
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class EditBusinessProfileUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val businessName: String = "",
    val categories: List<CategoryUi> = emptyList(),
    val selectedCategoryId: Int? = null,
    val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isFetchingLocation: Boolean = false,
    val descripcion: String = "",
    val schedule: List<DayScheduleUi> = emptyList(),
    val photos: List<PhotoUi> = emptyList(),
    val isUpdatingPhotos: Boolean = false,
    val photoError: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EditBusinessProfileViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val categoryApiService: CategoryApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val photoCompressor: PhotoCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBusinessProfileUiState())
    val uiState: StateFlow<EditBusinessProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                // coroutineScope hace que un error en cualquiera de las llamadas llegue al catch en vez de cancelar el launch.
                val (businessResponse, categoriesResponse) = coroutineScope {
                    val businessCall = async { businessApiService.getMyBusiness() }
                    val categoriesCall = async { categoryApiService.getCategories() }
                    businessCall.await() to categoriesCall.await()
                }
                val business = businessResponse.body()

                if (!businessResponse.isSuccessful || business == null || !categoriesResponse.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, loadError = "No pudimos cargar tu perfil") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        businessName = business.businessName.orEmpty(),
                        categories = categoriesResponse.body().orEmpty().map { c -> CategoryUi(c.id, c.name) },
                        selectedCategoryId = business.categoryId,
                        direccion = business.direccion.orEmpty(),
                        latitud = business.latitud,
                        longitud = business.longitud,
                        descripcion = business.descripcion.orEmpty(),
                        schedule = business.horarios.toScheduleUi(),
                        photos = business.fotos.toPhotoUi(BASE_URL),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun setBusinessName(value: String) {
        _uiState.update { it.copy(businessName = value) }
    }

    fun selectCategory(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setDireccion(value: String) {
        _uiState.update { it.copy(direccion = value) }
    }

    fun setDescripcion(value: String) {
        _uiState.update { it.copy(descripcion = value) }
    }

    fun toggleDay(diaSemana: Int) {
        _uiState.update { it.copy(schedule = it.schedule.toggleDay(diaSemana)) }
    }

    fun addSlot(diaSemana: Int) {
        _uiState.update { it.copy(schedule = it.schedule.addSlot(diaSemana)) }
    }

    fun removeSlot(diaSemana: Int, slotId: String) {
        _uiState.update { it.copy(schedule = it.schedule.removeSlot(diaSemana, slotId)) }
    }

    fun updateSlotTime(diaSemana: Int, slotId: String, horaInicio: String? = null, horaFin: String? = null) {
        _uiState.update { it.copy(schedule = it.schedule.updateSlotTime(diaSemana, slotId, horaInicio, horaFin)) }
    }

    fun copyMondayToAll() {
        _uiState.update { it.copy(schedule = it.schedule.copyMondayToAll()) }
    }

    @SuppressLint("MissingPermission") // el permiso ya se verifica antes de llamar esto, desde la screen
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, error = null) }
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build()
                val location = fusedLocationClient.getCurrentLocation(request, null).awaitLocation()
                if (location != null) {
                    _uiState.update {
                        it.copy(latitud = location.latitude, longitud = location.longitude, isFetchingLocation = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(isFetchingLocation = false, error = "No pudimos obtener tu ubicación, intentá de nuevo")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingLocation = false, error = "Error al obtener ubicación") }
            }
        }
    }

    private suspend fun Task<Location>.awaitLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
            addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
            addOnCanceledListener { if (cont.isActive) cont.cancel() }
        }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validationError = when {
            state.businessName.isBlank() -> "Ingresá el nombre del negocio"
            state.selectedCategoryId == null -> "Elegí un rubro"
            state.direccion.isBlank() -> "Ingresá la dirección"
            state.descripcion.isBlank() -> "Ingresá una descripción"
            else -> validateSchedule(state.schedule)
        }
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val response = businessApiService.updateMyBusiness(
                    UpdateBusinessRequestDto(
                        businessName = state.businessName.trim(),
                        categoryId = state.selectedCategoryId,
                        direccion = state.direccion.trim(),
                        latitud = state.latitud,
                        longitud = state.longitud,
                        descripcion = state.descripcion.trim(),
                        horarios = state.schedule.toScheduleRequest(),
                    ),
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "No pudimos guardar los cambios") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }

    // Las fotos se guardan apenas se agregan, reemplazan, borran o reordenan: no dependen de "Guardar cambios".

    fun addPhoto(uri: Uri) {
        if (_uiState.value.photos.size >= MAX_BUSINESS_PHOTOS) return
        runPhotoOperation {
            val part = photoPart(uri) ?: return@runPhotoOperation
            val response = businessApiService.addPhoto(part)
            val photo = response.body()
            if (response.isSuccessful && photo != null) {
                _uiState.update { it.copy(photos = it.photos + PhotoUi(photo.id, resolvePhotoUrl(BASE_URL, photo.url))) }
            } else {
                showPhotoError(response, "No pudimos subir la foto")
            }
        }
    }

    fun replacePhoto(photoId: Long, uri: Uri) {
        runPhotoOperation {
            val part = photoPart(uri) ?: return@runPhotoOperation
            val response = businessApiService.replacePhoto(photoId, part)
            val photo = response.body()
            if (response.isSuccessful && photo != null) {
                val newPhoto = PhotoUi(photo.id, resolvePhotoUrl(BASE_URL, photo.url))
                _uiState.update { state -> state.copy(photos = state.photos.map { if (it.id == photoId) newPhoto else it }) }
            } else {
                showPhotoError(response, "No pudimos reemplazar la foto")
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        runPhotoOperation {
            val response = businessApiService.deletePhoto(photoId)
            if (response.isSuccessful) {
                _uiState.update { state -> state.copy(photos = state.photos.filterNot { it.id == photoId }) }
            } else {
                showPhotoError(response, "No pudimos borrar la foto")
            }
        }
    }

    fun movePhoto(photoId: Long, offset: Int) {
        val reordered = _uiState.value.photos.moved(photoId, offset) ?: return
        runPhotoOperation {
            val response = businessApiService.reorderPhotos(ReorderPhotosRequestDto(reordered.map { it.id }))
            val photos = response.body()
            if (response.isSuccessful && photos != null) {
                _uiState.update { it.copy(photos = photos.toPhotoUi(BASE_URL)) }
            } else {
                showPhotoError(response, "No pudimos cambiar el orden")
            }
        }
    }

    private suspend fun photoPart(uri: Uri): MultipartBody.Part? {
        val bytes = photoCompressor.compress(uri)
        if (bytes == null) {
            _uiState.update { it.copy(photoError = "No pudimos leer la imagen elegida") }
            return null
        }
        return MultipartBody.Part.createFormData("file", "foto.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
    }

    private fun showPhotoError(response: Response<*>, fallback: String) {
        val message = when (response.code()) {
            409 -> "Podés tener hasta $MAX_BUSINESS_PHOTOS fotos"
            413 -> "La foto es demasiado grande"
            else -> fallback
        }
        _uiState.update { it.copy(photoError = message) }
    }

    private fun runPhotoOperation(block: suspend () -> Unit) {
        if (_uiState.value.isUpdatingPhotos) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPhotos = true, photoError = null) }
            try {
                block()
            } catch (e: Exception) {
                _uiState.update { it.copy(photoError = "Sin conexión, probá de nuevo") }
            } finally {
                _uiState.update { it.copy(isUpdatingPhotos = false) }
            }
        }
    }
}
