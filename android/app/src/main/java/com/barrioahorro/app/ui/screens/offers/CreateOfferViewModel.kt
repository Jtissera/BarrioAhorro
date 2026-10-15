package com.barrioahorro.app.ui.screens.offers

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.image.PhotoCompressor
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.offer.CreateOfferUseCase
import com.barrioahorro.app.domain.usecase.offer.UploadOfferPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject

enum class OfferType { PORCENTUAL, DOS_POR_UNO, POR_CANTIDAD, POR_MAYOR }

enum class VigenciaTipo(val label: String, val apiValue: String, val description: String) {
    HASTA_AGOTAR_STOCK("Hasta agotar stock", "HASTA_AGOTAR_STOCK", "Ideal para liquidar stock"),
    SOLO_HOY("Solo hoy", "SOLO_HOY", "Válida hasta las 23:59 de hoy"),
    TODA_LA_SEMANA("Toda la semana", "TODA_LA_SEMANA", "Válida durante 7 días corridos"),
    FECHA_ESPECIFICA("Fecha específica", "FECHA_ESPECIFICA", "Elegí el día y horario exacto"),
}

data class CreateOfferUiState(
    val selectedType: OfferType? = null,
    val productName: String = "",
    val price: String = "",
    val discountPercent: Int? = null,
    val requiredQuantity: Int = 2,
    val unitsToTake: Int = 2,
    val unitsToPay: Int = 1,
    val vigenciaTipo: VigenciaTipo = VigenciaTipo.HASTA_AGOTAR_STOCK,
    val validUntil: LocalDateTime? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val photoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val businessName: String = "",
    val categoryName: String = "",
) {
    val priceValue: BigDecimal? get() = price.replace(",", ".").toBigDecimalOrNull()

    /** Precio con descuento, solo para la vista previa. El precio real lo calcula el backend. */
    val previewDiscountedPrice: BigDecimal?
        get() {
            val p = priceValue ?: return null
            val d = discountPercent ?: return null
            return p.multiply(BigDecimal(100 - d)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        }

    /** Porcentaje de ahorro directo para promociones 2x1 o equivalente */
    val preview2x1SavingsPercent: Int
        get() = if (unitsToTake > unitsToPay && unitsToTake > 0) {
            ((unitsToTake - unitsToPay).toDouble() / unitsToTake * 100).toInt()
        } else {
            0
        }

    /** Precio total del paquete promocional (lo que paga el cliente) */
    val preview2x1TotalPromo: BigDecimal?
        get() = priceValue?.multiply(BigDecimal(unitsToPay))

    /** Precio unitario equivalente por unidad en 2x1 */
    val preview2x1UnitPrice: BigDecimal?
        get() = if (unitsToTake > 0) {
            preview2x1TotalPromo?.divide(BigDecimal(unitsToTake), 2, RoundingMode.HALF_UP)
        } else {
            null
        }

    val canSubmit: Boolean
        get() {
            if (isSubmitting || isUploadingPhoto || productName.isBlank()) return false
            val p = priceValue ?: return false
            if (p <= BigDecimal.ZERO) return false
            val vigenciaOk = vigenciaTipo != VigenciaTipo.FECHA_ESPECIFICA || validUntil != null
            if (!vigenciaOk) return false

            return when (selectedType) {
                OfferType.PORCENTUAL -> discountPercent != null
                OfferType.DOS_POR_UNO -> unitsToTake > unitsToPay && unitsToPay >= 1
                OfferType.POR_CANTIDAD -> discountPercent != null && requiredQuantity >= 2
                OfferType.POR_MAYOR, null -> false
            }
        }
}

sealed class CreateOfferEvent {
    data object Published : CreateOfferEvent()
}

@HiltViewModel
class CreateOfferViewModel @Inject constructor(
    private val createOfferUseCase: CreateOfferUseCase,
    private val uploadOfferPhotoUseCase: UploadOfferPhotoUseCase,
    private val photoCompressor: PhotoCompressor,
    private val businessApiService: BusinessApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOfferUiState())
    val uiState: StateFlow<CreateOfferUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateOfferEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadBusinessInfo()
    }

    private fun loadBusinessInfo() {
        viewModelScope.launch {
            try {
                val response = businessApiService.getMyBusiness()
                val business = response.body()
                if (response.isSuccessful && business != null) {
                    _uiState.update { current ->
                        current.copy(
                            businessName = business.businessName.orEmpty(),
                            categoryName = business.categoryName.orEmpty(),
                        )
                    }
                }
            } catch (_: Exception) {
                // Silencioso: si falla la conexión se mantendrán los valores por defecto
            }
        }
    }

    fun selectType(type: OfferType) = _uiState.update {
        it.copy(
            selectedType = type,
            error = null,
            // 2x1 arranca por defecto con hasta agotar stock (como en el mockup), porcentual con fecha específica
            vigenciaTipo = if (type == OfferType.DOS_POR_UNO) VigenciaTipo.HASTA_AGOTAR_STOCK else it.vigenciaTipo,
        )
    }
    fun clearType() = _uiState.update { current ->
        CreateOfferUiState(
            businessName = current.businessName,
            categoryName = current.categoryName,
        )
    }
    fun setProductName(value: String) = _uiState.update { it.copy(productName = value, error = null) }
    fun setPrice(value: String) = _uiState.update { it.copy(price = value, error = null) }
    fun setDiscount(value: Int?) = _uiState.update { it.copy(discountPercent = value, error = null) }
    fun setVigenciaTipo(value: VigenciaTipo) = _uiState.update { it.copy(vigenciaTipo = value, error = null) }
    fun setValidUntil(value: LocalDateTime?) = _uiState.update { it.copy(validUntil = value, error = null) }

    fun incrementQuantity() = _uiState.update { it.copy(requiredQuantity = it.requiredQuantity + 1) }
    fun decrementQuantity() = _uiState.update { it.copy(requiredQuantity = (it.requiredQuantity - 1).coerceAtLeast(2)) }

    fun setUnitsToTake(value: Int) = _uiState.update {
        val take = value.coerceAtLeast(2)
        val pay = if (it.unitsToPay >= take) take - 1 else it.unitsToPay
        it.copy(unitsToTake = take, unitsToPay = pay, error = null)
    }

    fun setUnitsToPay(value: Int) = _uiState.update {
        val pay = value.coerceIn(1, (it.unitsToTake - 1).coerceAtLeast(1))
        it.copy(unitsToPay = pay, error = null)
    }

    fun incrementUnitsToTake() = setUnitsToTake(_uiState.value.unitsToTake + 1)
    fun decrementUnitsToTake() = setUnitsToTake((_uiState.value.unitsToTake - 1).coerceAtLeast(2))

    fun incrementUnitsToPay() = setUnitsToPay((_uiState.value.unitsToPay + 1).coerceAtMost(_uiState.value.unitsToTake - 1))
    fun decrementUnitsToPay() = setUnitsToPay((_uiState.value.unitsToPay - 1).coerceAtLeast(1))

    fun submit() {
        val state = _uiState.value
        val offer = state.toNewOffer() ?: run {
            _uiState.update { it.copy(error = "Completá todos los campos obligatorios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = createOfferUseCase(offer)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CreateOfferEvent.Published)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.error.toMessage())
                }
            }
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, error = null) }

            val bytes = photoCompressor.compress(uri)
            if (bytes == null) {
                _uiState.update { it.copy(isUploadingPhoto = false, error = "No pudimos leer la foto") }
                return@launch
            }

            when (val result = uploadOfferPhotoUseCase(bytes)) {
                is Result.Success -> _uiState.update {
                    it.copy(isUploadingPhoto = false, photoUrl = result.value)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isUploadingPhoto = false, error = result.error.toMessage())
                }
            }
        }
    }

    fun removePhoto() = _uiState.update { it.copy(photoUrl = null) }

    private fun CreateOfferUiState.toNewOffer(): NewOffer? {
        val priceValue = price.replace(",", ".").toBigDecimalOrNull() ?: return null
        val until = if (vigenciaTipo == VigenciaTipo.FECHA_ESPECIFICA) validUntil ?: return null else null

        return when (selectedType) {
            OfferType.PORCENTUAL -> {
                val discount = discountPercent ?: return null
                NewOffer.Percentage(
                    productName = productName,
                    vigenciaTipo = vigenciaTipo.apiValue,
                    validUntil = until,
                    originalPrice = priceValue,
                    discountPercent = discount,
                    photoUrl = photoUrl,
                )
            }
            OfferType.POR_CANTIDAD -> {
                val discount = discountPercent ?: return null
                NewOffer.LastUnit(
                    productName = productName,
                    vigenciaTipo = vigenciaTipo.apiValue,
                    validUntil = until,
                    unitPrice = priceValue,
                    requiredQuantity = requiredQuantity,
                    lastUnitDiscountPercent = discount,
                    photoUrl = photoUrl,
                )
            }
            OfferType.DOS_POR_UNO -> {
                if (unitsToTake <= unitsToPay || unitsToPay < 1) return null
                NewOffer.TwoForOne(
                    productName = productName,
                    unitsToTake = unitsToTake.toShort(),
                    unitsToPay = unitsToPay.toShort(),
                    unitPrice = priceValue,
                    vigenciaTipo = vigenciaTipo.apiValue,
                    validUntil = until,
                    photoUrl = photoUrl,
                )
            }
            else -> null // to do: por mayor
        }
    }

    private fun OfferError.toMessage(): String = when (this) {
        OfferError.EmptyProductName -> "Ingresá el nombre del producto"
        OfferError.InvalidPrice -> "El precio tiene que ser mayor a $0"
        OfferError.InvalidPercent -> "El descuento tiene que estar entre 1% y 99%"
        OfferError.InvalidQuantity -> "La cantidad tiene que ser al menos 2"
        OfferError.PastDate -> "La fecha de fin tiene que ser posterior a ahora"
        is OfferError.Rejected -> message
        OfferError.NotABusiness -> "Tu sesión venció o no tenés permiso. Volvé a iniciar sesión"
        OfferError.NoConnection -> "Sin conexión, revisá tu internet"
        is OfferError.Unknown -> "Ocurrió un error inesperado"
    }
}

