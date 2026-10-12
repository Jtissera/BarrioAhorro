package com.barrioahorro.app.ui.screens.onboarding

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.location.ReverseGeocoder
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.dto.ScheduleSlotRequestDto
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.location.ValidateAddressUseCase
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CategoryUi(val id: Int, val name: String)

data class TimeSlotUi(
    val id: String = UUID.randomUUID().toString(),
    val horaInicio: String = "09:00",
    val horaFin: String = "19:00",
)

data class DayScheduleUi(
    val diaSemana: Int,
    val label: String,
    val enabled: Boolean,
    val slots: List<TimeSlotUi>,
)

private fun defaultSchedule(): List<DayScheduleUi> = listOf(
    DayScheduleUi(1, "Lunes", true, listOf(TimeSlotUi())),
    DayScheduleUi(2, "Martes", true, listOf(TimeSlotUi())),
    DayScheduleUi(3, "Miércoles", true, listOf(TimeSlotUi())),
    DayScheduleUi(4, "Jueves", true, listOf(TimeSlotUi())),
    DayScheduleUi(5, "Viernes", true, listOf(TimeSlotUi())),
    DayScheduleUi(6, "Sábado", false, emptyList()),
    DayScheduleUi(7, "Domingo", false, emptyList()),
)

data class OnboardingUiState(
    val businessName: String = "",
    val categories: List<CategoryUi> = emptyList(),
    val selectedCategoryId: Int? = null,
    val isLoadingCategories: Boolean = false,
    val direccion: String = "",
    val localidad: String = "",
    // Posición sugerida (Georef, GPS o pin movido por el vendedor). Todavía no es definitiva.
    val pinLatitud: Double? = null,
    val pinLongitud: Double? = null,
    // Posición que el vendedor confirmó con "Siguiente". Es la que se guarda en el backend.
    val confirmedLatitud: Double? = null,
    val confirmedLongitud: Double? = null,
    val isFetchingLocation: Boolean = false,
    val isValidatingAddress: Boolean = false,
    val isResolvingPinAddress: Boolean = false,
    val descripcion: String = "",
    val schedule: List<DayScheduleUi> = defaultSchedule(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val isLocationConfirmed: Boolean
        get() = confirmedLatitud != null && confirmedLongitud != null
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val categoryApiService: CategoryApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val validateAddressUseCase: ValidateAddressUseCase,
    private val reverseGeocoder: ReverseGeocoder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private var reverseGeocodeJob: Job? = null

    private companion object {
        // Si el pin se movió menos que esto y el reverse geocoding falla, conservamos el texto que ya había.
        const val PIN_NUDGE_METERS = 100f
    }

    fun setBusinessName(name: String) {
        _uiState.update { it.copy(businessName = name) }
    }

    fun loadCategories() {
        if (_uiState.value.categories.isNotEmpty() || _uiState.value.isLoadingCategories) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, error = null) }
            try {
                val response = categoryApiService.getCategories()
                if (response.isSuccessful) {
                    val categories = response.body().orEmpty().map { CategoryUi(it.id, it.name) }
                    _uiState.update { it.copy(categories = categories, isLoadingCategories = false) }
                } else {
                    _uiState.update { it.copy(isLoadingCategories = false, error = "No pudimos cargar los rubros") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCategories = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun selectCategory(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    // region Ubicación

    fun setDireccion(value: String) {
        // Escribir no invalida el pin: la dirección es un dato del comercio y el pin lo decide el vendedor.
        // Para volver a ubicar el pin a partir del texto se usa validateAddress() (tecla buscar).
        _uiState.update { it.copy(direccion = value, error = null) }
    }

    fun setLocalidad(value: String) {
        _uiState.update { it.copy(localidad = value, error = null) }
    }

    @SuppressLint("MissingPermission") // el permiso ya se verifica antes de llamar esto, desde la screen
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
                    // El GPS solo sugiere: el vendedor todavía tiene que revisar el mapa y confirmar.
                    _uiState.update {
                        it.copy(
                            pinLatitud = location.latitude,
                            pinLongitud = location.longitude,
                            isFetchingLocation = false,
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
        val prevLat = previous.pinLatitud
        val prevLng = previous.pinLongitud
        val movedLittle = prevLat != null && prevLng != null &&
                distanceMeters(prevLat, prevLng, latitud, longitud) < PIN_NUDGE_METERS

        _uiState.update { it.copy(pinLatitud = latitud, pinLongitud = longitud, error = null) }
        resolvePinAddress(latitud, longitud, keepTextOnFailure = movedLittle)
    }

    fun validateAddress() {
        val state = _uiState.value
        if (state.localidad.isBlank()) {
            _uiState.update { it.copy(error = "Ingresá la localidad para ubicar tu dirección con más precisión") }
            return
        }

        cancelPinResolution()
        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingAddress = true, error = null) }
            val result = validateAddressUseCase(
                address = state.direccion,
                locality = state.localidad,
            )
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        direccion = result.value.formattedAddress,
                        pinLatitud = result.value.latitude,
                        pinLongitud = result.value.longitude,
                        isValidatingAddress = false,
                    )
                }
                is Result.Failure -> _uiState.update {
                    // El pin que ya hubiera se conserva: el vendedor todavía puede ubicarlo a mano.
                    it.copy(
                        isValidatingAddress = false,
                        error = result.error.toMessage(),
                    )
                }
            }
        }
    }

    /** Acción explícita del vendedor: recién acá la ubicación sugerida pasa a confirmada. */
    fun confirmLocation(): Boolean {
        val state = _uiState.value
        val lat = state.pinLatitud
        val lng = state.pinLongitud
        if (lat == null || lng == null) {
            _uiState.update { it.copy(error = "Ubicá tu comercio en el mapa para continuar") }
            return false
        }
        if (state.direccion.isBlank() || state.localidad.isBlank()) {
            _uiState.update { it.copy(error = "Completá la dirección y la localidad de tu comercio para continuar") }
            return false
        }
        _uiState.update { it.copy(confirmedLatitud = lat, confirmedLongitud = lng, error = null) }
        return true
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
                    // Si el punto cambió pero no se pudo resolver la calle, no dejamos una dirección vieja.
                    direccion = nuevaDireccion,
                    // La localidad solo se pisa si el servicio devolvió una; si no, queda la que escribió el vendedor.
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

    // endregion

    fun setDescripcion(value: String) {
        _uiState.update { it.copy(descripcion = value) }
    }

    // region Horarios

    fun toggleDay(diaSemana: Int) {
        _uiState.update { state ->
            state.copy(
                schedule = state.schedule.map { day ->
                    if (day.diaSemana == diaSemana) {
                        val nowEnabled = !day.enabled
                        day.copy(
                            enabled = nowEnabled,
                            slots = if (nowEnabled && day.slots.isEmpty()) listOf(TimeSlotUi()) else day.slots,
                        )
                    } else {
                        day
                    }
                },
            )
        }
    }

    fun addSlot(diaSemana: Int) {
        _uiState.update { state ->
            state.copy(
                schedule = state.schedule.map { day ->
                    if (day.diaSemana == diaSemana) day.copy(slots = day.slots + TimeSlotUi()) else day
                },
            )
        }
    }

    fun removeSlot(diaSemana: Int, slotId: String) {
        _uiState.update { state ->
            state.copy(
                schedule = state.schedule.map { day ->
                    if (day.diaSemana == diaSemana) day.copy(slots = day.slots.filterNot { it.id == slotId }) else day
                },
            )
        }
    }

    fun updateSlotTime(diaSemana: Int, slotId: String, horaInicio: String? = null, horaFin: String? = null) {
        _uiState.update { state ->
            state.copy(
                schedule = state.schedule.map { day ->
                    if (day.diaSemana != diaSemana) return@map day
                    day.copy(
                        slots = day.slots.map { slot ->
                            if (slot.id != slotId) return@map slot
                            slot.copy(
                                horaInicio = horaInicio ?: slot.horaInicio,
                                horaFin = horaFin ?: slot.horaFin,
                            )
                        },
                    )
                },
            )
        }
    }

    fun copyMondayToAll() {
        _uiState.update { state ->
            val monday = state.schedule.firstOrNull { it.diaSemana == 1 } ?: return@update state
            state.copy(
                schedule = state.schedule.map { day ->
                    if (day.diaSemana == 1) {
                        day
                    } else {
                        day.copy(
                            enabled = monday.enabled,
                            slots = monday.slots.map { it.copy(id = UUID.randomUUID().toString()) },
                        )
                    }
                },
            )
        }
    }

    // endregion

    fun submitFull(onSuccess: () -> Unit) {
        val state = _uiState.value
        val categoryId = state.selectedCategoryId

        val horarios = state.schedule
            .filter { it.enabled }
            .flatMap { day ->
                day.slots.map { slot ->
                    ScheduleSlotRequestDto(
                        diaSemana = day.diaSemana,
                        horaInicio = slot.horaInicio,
                        horaFin = slot.horaFin,
                        activo = true,
                    )
                }
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val response = businessApiService.updateMyBusiness(
                    UpdateBusinessRequestDto(
                        businessName = state.businessName,
                        categoryId = categoryId,
                        direccion = state.direccion,
                        latitud = state.confirmedLatitud,
                        longitud = state.confirmedLongitud,
                        descripcion = state.descripcion,
                        horarios = horarios,
                    ),
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isSubmitting = false, error = "No pudimos guardar tu negocio") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }
}