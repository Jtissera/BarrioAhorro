package com.barrioahorro.app.ui.screens.onboarding

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.api.CategoryApiService
import com.barrioahorro.app.data.remote.dto.ScheduleSlotRequestDto
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.location.ValidateAddressUseCase

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
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isFetchingLocation: Boolean = false,
    val isValidatingAddress: Boolean = false,
    val isLocationConfirmed: Boolean = false,
    val descripcion: String = "",
    val schedule: List<DayScheduleUi> = defaultSchedule(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val categoryApiService: CategoryApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val validateAddressUseCase: ValidateAddressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

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
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCategories = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun selectCategory(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setDireccion(value: String) {
        _uiState.update {
            it.copy(
                direccion = value,
                latitud = null,
                longitud = null,
                isLocationConfirmed = false,
                error = null,
                )
        }
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
                        it.copy(
                            latitud = location.latitude,
                            longitud = location.longitude,
                            isFetchingLocation = false,
                            isLocationConfirmed = true,
                        )
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

    fun setDescripcion(value: String) {
        _uiState.update { it.copy(descripcion = value) }
    }

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
                        latitud = state.latitud,
                        longitud = state.longitud,
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
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = "Sin conexión, probá de nuevo") }
            }
        }
    }

    fun validateAddress() {
        val direccion = _uiState.value.direccion
        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingAddress = true, error = null) }
            when (val result = validateAddressUseCase(direccion)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        direccion = result.value.formattedAddress,
                        latitud = result.value.latitude,
                        longitud = result.value.longitude,
                        isLocationConfirmed = true,
                        isValidatingAddress = false,
                    )
                }
                is Result.Failure -> _uiState.update {
                    it.copy(
                        isValidatingAddress = false,
                        isLocationConfirmed = false,
                        error = result.error.toMessage(),
                    )
                }
            }
        }
    }

    private fun AddressError.toMessage(): String = when (this) {
        AddressError.Empty -> "Ingresá una dirección"
        AddressError.NotFound -> "No encontramos esa dirección. Revisá la calle y la altura"
        AddressError.MissingNumber -> "Agregá la altura de la calle a la dirección"
        AddressError.ServiceUnavailable -> "No pudimos validar la dirección ahora. Intentá más tarde"
        AddressError.NoConnection -> "Sin conexión, revisá tu internet"
        is AddressError.Unknown -> "Ocurrió un error inesperado"
    }

}