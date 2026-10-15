package com.barrioahorro.app.ui.screens.offers

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.image.PhotoCompressor
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

data class CreateOfferUiState(
    val selectedType: OfferType? = null,
    val productName: String = "",
    val price: String = "",
    val discountPercent: Int? = null,
    val requiredQuantity: Int = 2,
    val validUntil: LocalDateTime? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val photoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
) {
    private val priceValue: BigDecimal? get() = price.replace(",", ".").toBigDecimalOrNull()

    /** Precio con descuento, solo para la vista previa. El precio real lo calcula el backend. */
    val previewDiscountedPrice: BigDecimal?
        get() {
            val p = priceValue ?: return null
            val d = discountPercent ?: return null
            return p.multiply(BigDecimal(100 - d)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        }

    val canSubmit: Boolean
        get() = !isSubmitting && !isUploadingPhoto && productName.isNotBlank() && priceValue != null &&
                discountPercent != null && validUntil != null
}

sealed class CreateOfferEvent {
    data object Published : CreateOfferEvent()
}

@HiltViewModel
class CreateOfferViewModel @Inject constructor(
    private val createOfferUseCase: CreateOfferUseCase,
    private val uploadOfferPhotoUseCase: UploadOfferPhotoUseCase,
    private val photoCompressor: PhotoCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOfferUiState())
    val uiState: StateFlow<CreateOfferUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateOfferEvent>()
    val events = _events.receiveAsFlow()

    fun selectType(type: OfferType) = _uiState.update { it.copy(selectedType = type, error = null) }
    fun clearType() = _uiState.update { CreateOfferUiState() }
    fun setProductName(value: String) = _uiState.update { it.copy(productName = value, error = null) }
    fun setPrice(value: String) = _uiState.update { it.copy(price = value, error = null) }
    fun setDiscount(value: Int?) = _uiState.update { it.copy(discountPercent = value, error = null) }
    fun setValidUntil(value: LocalDateTime) = _uiState.update { it.copy(validUntil = value, error = null) }

    fun incrementQuantity() = _uiState.update { it.copy(requiredQuantity = it.requiredQuantity + 1) }
    fun decrementQuantity() = _uiState.update { it.copy(requiredQuantity = (it.requiredQuantity - 1).coerceAtLeast(2)) }

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
        val discount = discountPercent ?: return null
        val until = validUntil ?: return null
        return when (selectedType) {
            OfferType.PORCENTUAL -> NewOffer.Percentage(
                productName = productName,
                validUntil = until,
                originalPrice = priceValue,
                discountPercent = discount,
                photoUrl = photoUrl,
            )
            OfferType.POR_CANTIDAD -> NewOffer.LastUnit(
                productName = productName,
                validUntil = until,
                unitPrice = priceValue,
                requiredQuantity = requiredQuantity,
                lastUnitDiscountPercent = discount,
                photoUrl = photoUrl,
            )
            else -> null // to do: 2x1 y por mayor
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

