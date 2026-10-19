package com.barrioahorro.app.ui.screens.client.location

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import com.barrioahorro.app.data.location.ReverseGeocoder
import com.barrioahorro.app.domain.model.AddressCandidate
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.location.SearchAddressCandidatesUseCase
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LocationStep { INTRO, ADDRESS }

data class ClientLocationUiState(
    val step: LocationStep = LocationStep.INTRO,
    /** Texto del campo, con el formato "calle altura, localidad". */
    val query: String = "",
    val candidates: List<AddressCandidate> = emptyList(),
    val pinLatitude: Double? = null,
    val pinLongitude: Double? = null,
    val label: String = "",
    val approximate: Boolean = false,
    val gpsFailed: Boolean = false,
    val isFetchingGps: Boolean = false,
    val isSearching: Boolean = false,
    val isResolvingPin: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val hasPin: Boolean get() = pinLatitude != null && pinLongitude != null
    val canConfirm: Boolean get() = hasPin && !isSaving && !isSearching
}

private const val PIN_IGNORE_METERS = 10f
private const val MAX_LABEL = 120

@HiltViewModel
class ClientLocationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationStore: ClientLocationDataStore,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val searchCandidates: SearchAddressCandidatesUseCase,
    private val reverseGeocoder: ReverseGeocoder,
) : ViewModel() {

    /** true cuando se llega desde "cambiar ubicación": arranca en el paso de dirección. */
    val isEditing: Boolean = savedStateHandle.get<Boolean>("edit") ?: false

    private val _uiState = MutableStateFlow(
        ClientLocationUiState(step = if (isEditing) LocationStep.ADDRESS else LocationStep.INTRO),
    )
    val uiState: StateFlow<ClientLocationUiState> = _uiState.asStateFlow()

    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var searchJob: Job? = null
    private var reverseJob: Job? = null

    init {
        if (isEditing) {
            viewModelScope.launch {
                locationStore.location.first()?.let { saved ->
                    _uiState.update {
                        it.copy(
                            pinLatitude = saved.latitude,
                            pinLongitude = saved.longitude,
                            label = saved.label,
                            query = saved.label,
                        )
                    }
                }
            }
        }
    }

    // ---- Paso 1: intro --------------------------------------------------------------

    fun goToManual() {
        _uiState.update { it.copy(step = LocationStep.ADDRESS, error = null) }
    }

    fun backToIntro() {
        _uiState.update { it.copy(step = LocationStep.INTRO, error = null) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(step = LocationStep.ADDRESS, gpsFailed = true) }
    }

    /** "Ahora no": se recuerda la decisión para no volver a preguntar, y se puede cambiar después. */
    fun skip() {
        viewModelScope.launch {
            locationStore.markDeclined()
            _events.send(Unit)
        }
    }

    @SuppressLint("MissingPermission") // la pantalla verifica el permiso antes de llamar
    fun useGps() {
        if (_uiState.value.isFetchingGps) return
        _uiState.update { it.copy(isFetchingGps = true, gpsFailed = false, error = null) }

        viewModelScope.launch {
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build()
                val location = fusedLocationClient.getCurrentLocation(request, null).awaitLocation()
                if (location == null) {
                    gpsFailed()
                    return@launch
                }

                val resolved = reverseGeocoder.addressFor(location.latitude, location.longitude)
                val label = labelFrom(resolved?.street, resolved?.locality) ?: "Mi ubicación actual"
                locationStore.save(location.latitude, location.longitude, label)
                _uiState.update { it.copy(isFetchingGps = false) }
                _events.send(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                gpsFailed()
            }
        }
    }

    private fun gpsFailed() {
        _uiState.update {
            it.copy(isFetchingGps = false, step = LocationStep.ADDRESS, gpsFailed = true)
        }
    }

    private suspend fun Task<Location>.awaitLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
            addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
            addOnCanceledListener { if (cont.isActive) cont.cancel() }
        }

    // ---- Paso 2: dirección ----------------------------------------------------------

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value.take(MAX_LABEL), error = null) }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _uiState.update { it.copy(query = "", candidates = emptyList(), error = null) }
    }

    /** Solo al tocar "buscar" en el teclado: un pedido por búsqueda, nunca por tecla. */
    fun search() {
        val raw = _uiState.value.query.trim()
        if (raw.isEmpty()) {
            _uiState.update { it.copy(error = "Ingresá una dirección") }
            return
        }
        val (street, locality) = splitQuery(raw)

        searchJob?.cancel()
        reverseJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, isResolvingPin = false, error = null, candidates = emptyList()) }

            when (val result = searchCandidates(street, locality)) {
                is Result.Success -> {
                    val list = result.value
                    if (list.isEmpty()) {
                        _uiState.update {
                            it.copy(isSearching = false, pinLatitude = null, pinLongitude = null, error = ERROR_NOT_FOUND)
                        }
                    } else {
                        val first = list.first()
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                candidates = list,
                                pinLatitude = first.latitude,
                                pinLongitude = first.longitude,
                                label = labelFrom(first.label, first.locality).orEmpty(),
                                approximate = !first.exact,
                            )
                        }
                    }
                }

                is Result.Failure -> _uiState.update {
                    it.copy(
                        isSearching = false,
                        pinLatitude = null,
                        pinLongitude = null,
                        error = result.error.toMessage(),
                    )
                }
            }
        }
    }

    fun selectCandidate(candidate: AddressCandidate) {
        val text = labelFrom(candidate.label, candidate.locality).orEmpty()
        _uiState.update {
            it.copy(
                query = text,
                label = text,
                candidates = emptyList(),
                pinLatitude = candidate.latitude,
                pinLongitude = candidate.longitude,
                approximate = !candidate.exact,
                error = null,
            )
        }
    }

    /** El mapa dejó de moverse: el pin decide la ubicación y se intenta mostrar su dirección. */
    fun onPinMoved(latitude: Double, longitude: Double) {
        val current = _uiState.value
        val prevLat = current.pinLatitude
        val prevLng = current.pinLongitude
        // El mapa también avisa cuando lo centramos nosotros: eso no es un movimiento del usuario.
        if (prevLat != null && prevLng != null && distanceMeters(prevLat, prevLng, latitude, longitude) < PIN_IGNORE_METERS) {
            return
        }

        searchJob?.cancel()
        _uiState.update {
            it.copy(
                pinLatitude = latitude,
                pinLongitude = longitude,
                candidates = emptyList(),
                approximate = false,
                error = null,
            )
        }

        reverseJob?.cancel()
        reverseJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolvingPin = true) }
            val resolved = reverseGeocoder.addressFor(latitude, longitude)
            val text = labelFrom(resolved?.street, resolved?.locality)
            _uiState.update {
                it.copy(
                    isResolvingPin = false,
                    label = text ?: it.label,
                    query = text ?: it.query,
                )
            }
        }
    }

    fun confirm() {
        val state = _uiState.value
        val lat = state.pinLatitude ?: return
        val lng = state.pinLongitude ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val label = state.label.ifBlank { state.query.ifBlank { "Mi ubicación" } }.take(MAX_LABEL)
            locationStore.save(lat, lng, label)
            _uiState.update { it.copy(isSaving = false) }
            _events.send(Unit)
        }
    }

    // ---- Utilidades -----------------------------------------------------------------

    private fun splitQuery(raw: String): Pair<String, String?> {
        val comma = raw.lastIndexOf(',')
        if (comma < 0) return raw to null
        val street = raw.substring(0, comma).trim()
        val locality = raw.substring(comma + 1).trim().takeIf { it.isNotEmpty() }
        return if (street.isEmpty()) raw to null else street to locality
    }

    private fun labelFrom(street: String?, locality: String?): String? {
        val parts = listOfNotNull(street?.takeIf { it.isNotBlank() }, locality?.takeIf { it.isNotBlank() })
        return parts.joinToString(", ").takeIf { it.isNotEmpty() }
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    private fun AddressError.toMessage(): String = when (this) {
        AddressError.Empty -> "Ingresá una dirección"
        AddressError.NotFound, AddressError.MissingNumber -> ERROR_NOT_FOUND
        AddressError.ServiceUnavailable -> "No pudimos buscar la dirección ahora. Mové el pin en el mapa o intentá más tarde"
        AddressError.NoConnection -> "Sin conexión, revisá tu internet"
        is AddressError.Unknown -> "Ocurrió un error inesperado"
    }

    private companion object {
        const val ERROR_NOT_FOUND = "No encontramos esa dirección. Revisá y probá de nuevo."
    }
}