package com.barrioahorro.app.ui.screens.business

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.location.ReverseGeocoder
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.location.ValidateAddressUseCase
import com.barrioahorro.app.ui.screens.onboarding.CategoryUi
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
    val localidad: String = "",
    val resolvedDireccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isFetchingLocation: Boolean = false,
    val isValidatingAddress: Boolean = false,
    val isResolvingPinAddress: Boolean = false,
    val alturaEncontrada: Boolean? = null,
    val mensajeValidacion: String? = null,
    val descripcion: String = "",
    val schedule: List<DayScheduleUi> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val addressNeedsSearch: Boolean
        get() = direccion.trim() != resolvedDireccion.trim()

    val isResolvingLocation: Boolean
        get() = isFetchingLocation || isValidatingAddress || isResolvingPinAddress
}

@HiltViewModel
class EditBusinessProfileViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val categoryApiService: CategoryApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val validateAddressUseCase: ValidateAddressUseCase,
    private val reverseGeocoder: ReverseGeocoder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBusinessProfileUiState())
    val uiState: StateFlow<EditBusinessProfileUiState> = _uiState

    private var reverseGeocodeJob: Job? = null

    private companion object {
        const val PIN_NUDGE_METERS = 100f
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
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
                        resolvedDireccion = business.direccion.orEmpty(),
                        latitud = business.latitud,
                        longitud = business.longitud,
                        descripcion = business.descripcion.orEmpty(),
                        schedule = business.horarios.toScheduleUi(),
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

    fun setDescripcion(value: String) {
        _uiState.update { it.copy(descripcion = value) }
    }


    fun setDireccion(value: String) {
        _uiState.update { it.copy(direccion = value, error = null, alturaEncontrada = null, mensajeValidacion = null) }
    }

    fun setLocalidad(value: String) {
        _uiState.update { it.copy(localidad = value, error = null) }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        cancelPinResolution()
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, error = null) }
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build()
                val location = fusedLocationClient.getCurrentLocation(request, null).awaitLocation()
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            latitud = location.latitude,
                            longitud = location.longitude,
                            isFetchingLocation = false,
                            alturaEncontrada = null,
                            mensajeValidacion = null,
                        )
                    }
                    resolvePinAddress(location.latitude, location.longitude, keepTextOnFailure = false)
                } else {
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            error = "No pudimos obtener tu ubicación. Activá el GPS o ingresá tu dirección",
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        error = "Sin permiso de ubicación. Ingresá tu dirección o ubicá el pin en el mapa",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        error = "No pudimos obtener tu ubicación. Ingresá tu dirección o ubicá el pin en el mapa",
                    )
                }
            }
        }
    }

    private suspend fun Task<Location>.awaitLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
            addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
            addOnCanceledListener { if (cont.isActive) cont.cancel() }
        }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(error = "Sin permiso de ubicación. Ingresá tu dirección o ubicá el pin en el mapa")
        }
    }

    fun onPinMoved(latitud: Double, longitud: Double) {
        val previous = _uiState.value
        val prevLat = previous.latitud
        val prevLng = previous.longitud
        val movedLittle = prevLat != null && prevLng != null &&
                distanceMeters(prevLat, prevLng, latitud, longitud) < PIN_NUDGE_METERS

        _uiState.update {
            it.copy(latitud = latitud, longitud = longitud, error = null, alturaEncontrada = null, mensajeValidacion = null)
        }
        resolvePinAddress(latitud, longitud, keepTextOnFailure = movedLittle)
    }

    fun validateAddress() {
        val state = _uiState.value

        if (state.localidad.isBlank()) {
            _uiState.update {
                it.copy(
                    error = "Ingresá la localidad para ubicar tu dirección con más precisión",
                    alturaEncontrada = null,
                    mensajeValidacion = null,
                )
            }
            return
        }

        cancelPinResolution()

        viewModelScope.launch {
            _uiState.update {
                it.copy(isValidatingAddress = true, error = null, alturaEncontrada = null, mensajeValidacion = null)
            }

            try {
                when (val result = validateAddressUseCase(address = state.direccion, locality = state.localidad)) {
                    is Result.Success -> {
                        val validated = result.value
                        _uiState.update {
                            it.copy(
                                direccion = validated.formattedAddress,
                                resolvedDireccion = validated.formattedAddress,
                                latitud = validated.latitude,
                                longitud = validated.longitude,
                                alturaEncontrada = validated.alturaEncontrada,
                                mensajeValidacion = validated.mensaje,
                                isValidatingAddress = false,
                                error = null,
                            )
                        }
                    }

                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                isValidatingAddress = false,
                                alturaEncontrada = null,
                                mensajeValidacion = null,
                                error = result.error.toMessage(),
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isValidatingAddress = false,
                        alturaEncontrada = null,
                        mensajeValidacion = null,
                        error = "No pudimos validar la dirección. Intentá de nuevo",
                    )
                }
            }
        }
    }

    private fun resolvePinAddress(latitud: Double, longitud: Double, keepTextOnFailure: Boolean) {
        cancelPinResolution()
        reverseGeocodeJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolvingPinAddress = true) }
            val resolved = reverseGeocoder.addressFor(latitud, longitud)
            _uiState.update {
                val nuevaDireccion = resolved?.street
                    ?: if (keepTextOnFailure) it.direccion else ""
                it.copy(
                    isResolvingPinAddress = false,
                    direccion = nuevaDireccion,
                    resolvedDireccion = nuevaDireccion,
                    // La localidad solo se pisa si el servicio devolvió una.
                    localidad = resolved?.locality ?: it.localidad,
                )
            }
        }
    }

    private fun cancelPinResolution() {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = null
        _uiState.update { it.copy(isResolvingPinAddress = false) }
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    private fun AddressError.toMessage(): String = when (this) {
        AddressError.Empty -> "Ingresá una dirección"
        AddressError.NotFound -> "No encontramos esa dirección. Revisá la calle y la altura, o ubicá el pin en el mapa"
        AddressError.MissingNumber -> "Agregá la altura de la calle a la dirección"
        AddressError.ServiceUnavailable -> "No pudimos validar la dirección ahora. Ubicá el pin en el mapa o intentá más tarde"
        AddressError.NoConnection -> "Sin conexión, revisá tu internet"
        is AddressError.Unknown -> "Ocurrió un error inesperado"
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


    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validationError = when {
            state.businessName.isBlank() -> "Ingresá el nombre del negocio"
            state.selectedCategoryId == null -> "Elegí un rubro"
            state.direccion.isBlank() -> "Ingresá la dirección"
            state.isResolvingLocation -> "Esperá a que termine de ubicarse la dirección"
            state.addressNeedsSearch ->
                "Cambiaste la dirección: tocá Buscar para ubicarla en el mapa o mové el pin"
            state.latitud == null || state.longitud == null -> "Ubicá tu comercio en el mapa"
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
}