package com.barrioahorro.app.ui.screens.offers

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.image.PhotoCompressor
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.domain.usecase.offer.CreateOfferUseCase
import com.barrioahorro.app.domain.usecase.offer.UploadOfferPhotoUseCase
import com.barrioahorro.app.ui.components.OfferPricing
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
import java.time.OffsetDateTime
import java.time.ZoneId
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
    val wholesalePrice: String = "",
    val minQuantity: Int = 6,
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
    /** Distinto de null: se está editando esa oferta. */
    val editingId: Long? = null,
    val isLoadingOffer: Boolean = false,
    val loadError: String? = null,
    val isDeleting: Boolean = false,
    /** Distinto de null: la oferta recién creada, para mostrar la pantalla de "publicada". */
    val publishedOfferId: Long? = null,
) {
    val priceValue: BigDecimal? get() = price.replace(",", ".").toBigDecimalOrNull()
    val wholesalePriceValue: BigDecimal? get() = wholesalePrice.replace(",", ".").toBigDecimalOrNull()

    /** Precio con descuento, solo para la vista previa. El precio real lo calcula el backend. */

    val previewDiscountedPrice: BigDecimal?
        get() {
            val p = priceValue ?: return null
            val d = discountPercent ?: return null
            return OfferPricing.percentageFinal(p, d)
        }

    val preview2x1SavingsPercent: Int
        get() = if (unitsToTake > unitsToPay && unitsToTake > 0) {
            ((unitsToTake - unitsToPay).toDouble() / unitsToTake * 100).toInt()
        } else {
            0
        }

    val preview2x1TotalPromo: BigDecimal?
        get() = priceValue?.multiply(BigDecimal(unitsToPay))

    val preview2x1UnitPrice: BigDecimal?
        get() = priceValue?.let { OfferPricing.twoForOneUnit(it, unitsToTake, unitsToPay) }

    val previewWholesaleSavingsPercent: Int?
        get() {
            val reg = priceValue ?: return null
            val wh = wholesalePriceValue ?: return null
            return OfferPricing.wholesalePercent(reg, wh)
        }

    val canSubmit: Boolean
        get() {
            if (isSubmitting || isDeleting || isUploadingPhoto || productName.isBlank()) return false
            val p = priceValue ?: return false
            if (p <= BigDecimal.ZERO) return false
            val vigenciaOk = vigenciaTipo != VigenciaTipo.FECHA_ESPECIFICA || validUntil != null
            if (!vigenciaOk) return false

            return when (selectedType) {
                OfferType.PORCENTUAL -> discountPercent != null
                OfferType.DOS_POR_UNO -> unitsToTake > unitsToPay && unitsToPay >= 1
                OfferType.POR_CANTIDAD -> discountPercent != null && requiredQuantity >= 2
                OfferType.POR_MAYOR -> {
                    val wh = wholesalePriceValue
                    wh != null && wh > BigDecimal.ZERO && wh < p && minQuantity >= 2
                }
                null -> false
            }
        }
}

/** Eventos de una sola vez; la publicación se refleja en el estado (publishedOfferId). */
sealed class CreateOfferEvent {
    data object Saved : CreateOfferEvent()
    data object Deleted : CreateOfferEvent()
}

@HiltViewModel
class CreateOfferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createOfferUseCase: CreateOfferUseCase,
    private val uploadOfferPhotoUseCase: UploadOfferPhotoUseCase,
    private val photoCompressor: PhotoCompressor,
    private val businessApiService: BusinessApiService,
    private val offerRepository: IOfferRepository,
) : ViewModel() {

    /** Argumento "offerId" de la ruta de edición; null al crear. */
    private val editingId: Long? = savedStateHandle.get<Long>("offerId")

    private val _uiState = MutableStateFlow(
        CreateOfferUiState(editingId = editingId, isLoadingOffer = editingId != null),
    )
    val uiState: StateFlow<CreateOfferUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateOfferEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadBusinessInfo()
        editingId?.let(::loadOffer)
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

    fun retryLoad() {
        editingId?.let(::loadOffer)
    }

    private fun loadOffer(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOffer = true, loadError = null) }
            when (val result = offerRepository.getOffer(id)) {
                is Result.Success -> _uiState.update { result.value.toFormState(it) }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoadingOffer = false, loadError = result.error.toMessage())
                }
            }
        }
    }

    fun selectType(type: OfferType) = _uiState.update {
        it.copy(
            selectedType = type,
            error = null,
            vigenciaTipo = if (type == OfferType.DOS_POR_UNO || type == OfferType.POR_MAYOR) VigenciaTipo.HASTA_AGOTAR_STOCK else it.vigenciaTipo,
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

    fun setWholesalePrice(value: String) = _uiState.update { it.copy(wholesalePrice = value, error = null) }
    fun setMinQuantity(value: Int) = _uiState.update { it.copy(minQuantity = value.coerceAtLeast(2), error = null) }
    fun incrementMinQuantity() = setMinQuantity(_uiState.value.minQuantity + 1)
    fun decrementMinQuantity() = setMinQuantity((_uiState.value.minQuantity - 1).coerceAtLeast(2))

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val offer = state.toNewOffer() ?: run {
            _uiState.update { it.copy(error = "Completá todos los campos obligatorios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val id = state.editingId
            if (id != null) {
                when (val result = offerRepository.updateOffer(id, offer)) {
                    is Result.Success -> _uiState.update {
                        it.copy(isSubmitting = false, publishedOfferId = id)
                    }
                    is Result.Failure -> _uiState.update {
                        it.copy(isSubmitting = false, error = result.error.toMessage())
                    }
                }
            } else {
                when (val result = createOfferUseCase(offer)) {
                    is Result.Success -> _uiState.update {
                        it.copy(isSubmitting = false, publishedOfferId = result.value)
                    }
                    is Result.Failure -> _uiState.update {
                        it.copy(isSubmitting = false, error = result.error.toMessage())
                    }
                }
            }
        }
    }

    fun delete() {
        val id = _uiState.value.editingId ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            when (val result = offerRepository.deleteOffer(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(CreateOfferEvent.Deleted)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isDeleting = false, error = result.error.toMessage())
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
            OfferType.POR_MAYOR -> {
                val wh = wholesalePrice.replace(",", ".").toBigDecimalOrNull() ?: return null
                if (wh <= BigDecimal.ZERO || priceValue <= BigDecimal.ZERO || wh >= priceValue || minQuantity < 2) return null
                NewOffer.Wholesale(
                    productName = productName,
                    minQuantity = minQuantity.toShort(),
                    wholesalePrice = wh,
                    regularPrice = priceValue,
                    vigenciaTipo = vigenciaTipo.apiValue,
                    validUntil = until,
                    photoUrl = photoUrl,
                )
            }
            null -> null
        }
    }

    /** Carga en el formulario los datos de una oferta existente. */
    private fun OfferResponseDto.toFormState(base: CreateOfferUiState): CreateOfferUiState {
        val vigencia = VigenciaTipo.entries.firstOrNull { it.apiValue == vigenciaTipo }
            ?: VigenciaTipo.HASTA_AGOTAR_STOCK
        val until = if (vigencia == VigenciaTipo.FECHA_ESPECIFICA) {
            vigenteHasta?.let {
                runCatching {
                    OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                }.getOrNull()
            }
        } else {
            null
        }
        val common = base.copy(
            productName = nombreProducto,
            photoUrl = fotoUrl,
            vigenciaTipo = vigencia,
            validUntil = until,
            isLoadingOffer = false,
            loadError = null,
        )

        val loaded = when (tipoOferta) {
            "PORCENTAJE" -> detallePorcentaje?.let {
                common.copy(
                    selectedType = OfferType.PORCENTUAL,
                    price = it.precioOriginal.plain(),
                    discountPercent = it.porcentajeDescuento.toInt(),
                )
            }
            "DOS_POR_UNO" -> detalle2x1?.let {
                common.copy(
                    selectedType = OfferType.DOS_POR_UNO,
                    price = it.precioUnitario?.plain().orEmpty(),
                    unitsToTake = it.unidadesALlevar.toInt(),
                    unitsToPay = it.unidadesAPagar.toInt(),
                )
            }
            "CANTIDAD_ULTIMA_UNIDAD" -> detalleCantidad?.let {
                common.copy(
                    selectedType = OfferType.POR_CANTIDAD,
                    price = it.precioUnitario.plain(),
                    requiredQuantity = it.cantidadRequerida.toInt(),
                    discountPercent = it.porcentajeDescuentoUltimaUnidad.toInt(),
                )
            }
            "POR_MAYOR" -> detallePorMayor?.let {
                common.copy(
                    selectedType = OfferType.POR_MAYOR,
                    price = it.precioUnitarioRegular?.plain().orEmpty(),
                    wholesalePrice = it.precioUnitarioMayorista.plain(),
                    minQuantity = it.cantidadMinima.toInt(),
                )
            }
            else -> null
        }
        return loaded ?: base.copy(isLoadingOffer = false, loadError = "No pudimos leer esta oferta")
    }

    private fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()

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