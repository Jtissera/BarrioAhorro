package com.barrioahorro.app.ui.screens.client.offer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.explore.ExploreRepository
import com.barrioahorro.app.data.explore.OfferDetailDto
import com.barrioahorro.app.data.local.datastore.ClientLocationDataStore
import com.barrioahorro.app.di.BASE_URL
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import com.barrioahorro.app.ui.components.OfferPriceInfo
import com.barrioahorro.app.ui.components.ordinal
import com.barrioahorro.app.ui.screens.business.resolvePhotoUrl
import com.barrioahorro.app.ui.screens.client.explore.ExploreOfferMapper
import com.barrioahorro.app.ui.screens.client.explore.FavoritesSync
import com.barrioahorro.app.ui.screens.client.forum.formatRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
data class OfferDetailUi(
    val id: Long,
    val businessId: Long,
    val productName: String,
    val photoUrl: String?,
    val badgeText: String,
    val live: Boolean,
    val favorite: Boolean,
    val price: BigDecimal?,
    val originalPrice: BigDecimal?,
    val savings: BigDecimal?,
    val endsInText: String?,
    val ruleText: String,
    val validityText: String,
    val dateText: String?,
    val endedText: String?,
    val confirmations: Int,
    val confirmedByMe: Boolean = false,
    val lastConfirmationText: String?,
    val businessName: String,
    val categoryName: String,
    val businessPhotoUrl: String?,
    val businessAddress: String,
    val distanceText: String,
)

data class OfferDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val offer: OfferDetailUi? = null,
)

object OfferDetailMapper {

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun toUi(d: OfferDetailDto, now: OffsetDateTime = OffsetDateTime.now()): OfferDetailUi {
        val o = d.offer
        val card = ExploreOfferMapper.toUi(o).card
        val (price, original) = prices(card.priceInfo)
        val savings = if (price != null && original != null && original > price) original.subtract(price) else null

        val validUntil = parse(o.validUntil)
        val endedAt = parse(d.endedAt ?: o.validUntil)

        return OfferDetailUi(
            id = o.id,
            businessId = o.businessId,
            productName = o.productName,
            photoUrl = o.photoUrl?.let { resolvePhotoUrl(BASE_URL, it) },
            badgeText = card.badgeText.replace("\n", " "),
            live = o.live,
            favorite = o.favorite,
            price = price,
            originalPrice = original,
            savings = savings,
            endsInText = endsIn(o.validUntil, o.live, now),
            ruleText = rule(o),
            validityText = when (o.validityType) {
                "SOLO_HOY" -> "Solo por hoy"
                "TODA_LA_SEMANA" -> "Toda la semana"
                "FECHA_ESPECIFICA" -> "Con fecha de vencimiento"
                else -> "Hasta agotar stock"
            },
            dateText = if (o.live) {
                validUntil?.let { "Vigente hasta ${it.format(dateFmt)} • ${it.format(timeFmt)} hs" }
            } else {
                endedAt?.let { "Finalizó el ${it.format(dateFmt)} a las ${it.format(timeFmt)} hs" }
            },
            endedText = endedAt?.let { "Finalizó el ${it.format(dateFmt)}" },
            confirmations = o.confirmations,
            confirmedByMe = o.validatedByMe ?: false,
            lastConfirmationText = d.lastConfirmation?.let {
                "Última confirmación: ${formatRelativeTime(it.at)} por ${it.by}"
            },
            businessName = o.businessName ?: "Comercio",
            categoryName = o.category.orEmpty(),
            businessPhotoUrl = d.businessPhotoUrl?.let { resolvePhotoUrl(BASE_URL, it) },
            businessAddress = d.businessAddress.orEmpty(),
            distanceText = card.distanceText,
        )
    }

    /** (precio actual por unidad, precio de lista). */
    private fun prices(info: OfferPriceInfo): Pair<BigDecimal?, BigDecimal?> = when (info) {
        OfferPriceInfo.Empty -> null to null
        is OfferPriceInfo.Percentage -> info.discountedPrice to info.regularPrice
        is OfferPriceInfo.TwoForOne -> info.unitPrice to info.regularPrice
        is OfferPriceInfo.Quantity -> {
            val n = info.requiredQuantity
            val average = info.regularPrice.multiply(BigDecimal(n - 1))
                .add(info.discountedPrice)
                .divide(BigDecimal(n), 0, RoundingMode.HALF_UP)
            average to info.regularPrice
        }
        is OfferPriceInfo.Wholesale -> info.wholesalePrice to info.regularPrice?.takeIf { it > info.wholesalePrice }
    }

    private fun rule(o: com.barrioahorro.app.data.explore.ExploreOfferDto): String = when (o.type) {
        "PORCENTAJE" -> "${o.percent ?: 0}% de descuento sobre el precio de lista"
        "DOS_POR_UNO" -> "Llevando ${o.take ?: 2}, pagás ${o.pay ?: 1}"
        "CANTIDAD_ULTIMA_UNIDAD" -> {
            val n = o.requiredQuantity ?: 2
            "Llevando $n, la ${ordinal(n)} con ${o.lastUnitDiscount ?: 0}% de descuento"
        }
        "POR_MAYOR" -> "Desde ${o.minQuantity ?: 2} unidades, precio por mayor"
        else -> "Promoción del comercio"
    }

    private fun endsIn(validUntil: String?, live: Boolean, now: OffsetDateTime): String? {
        if (!live || validUntil == null) return null
        val end = runCatching { OffsetDateTime.parse(validUntil) }.getOrNull() ?: return null
        val minutes = Duration.between(now, end).toMinutes()
        return when {
            minutes <= 0 -> null
            minutes < 60 -> "Termina en $minutes min"
            minutes < 24 * 60 -> "Termina en ${(minutes + 59) / 60} hs"
            else -> null
        }
    }

    private fun parse(value: String?): ZonedDateTime? = value?.let {
        runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()
    }
}

@HiltViewModel
class OfferDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExploreRepository,
    private val offerRepository: IOfferRepository,
    private val locationStore: ClientLocationDataStore,
    private val favoritesSync: FavoritesSync,
) : ViewModel() {

    private val offerId: Long = checkNotNull(savedStateHandle.get<Long>("offerId"))

    private val _state = MutableStateFlow(OfferDetailUiState())
    val state: StateFlow<OfferDetailUiState> = _state.asStateFlow()

    private var favoriteInFlight = false

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val location = locationStore.location.first()

            repository.getOfferDetail(offerId, location?.latitude, location?.longitude)
                .onSuccess { dto ->
                    val baseUi = OfferDetailMapper.toUi(dto)

                    val validationResult = offerRepository.getOfferValidations(offerId)

                    val finalUi = when (validationResult) {
                        is Result.Success -> {
                            val summary = validationResult.value
                            baseUi.copy(
                                confirmations = summary.totalValidations.toInt(),
                                confirmedByMe = summary.validatedByMe,
                                lastConfirmationText = if (summary.validatedByMe) "Última confirmación: recién por vos" else baseUi.lastConfirmationText
                            )
                        }
                        is Result.Failure -> {
                            baseUi
                        }
                    }

                    _state.update { it.copy(isLoading = false, offer = finalUi) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "No pudimos cargar la oferta") }
                }
        }
    }
    fun confirmValidity() {
        val currentOffer = _state.value.offer ?: return
        if (currentOffer.confirmedByMe) return

        viewModelScope.launch {
            when (val result = offerRepository.validateOffer(currentOffer.id)) {
                is Result.Success -> {
                    val summary = result.value
                    _state.update { current ->
                        current.copy(
                            offer = current.offer?.copy(
                                confirmations = summary.totalValidations.toInt(),
                                confirmedByMe = summary.validatedByMe,
                                lastConfirmationText = "Última confirmación: recién por vos",
                            ),
                            message = "¡Gracias por ayudar al barrio!",
                        )
                    }
                }
                is Result.Failure -> {
                    val errorMsg = when (val err = result.error) {
                        is OfferError.NoConnection -> "Sin conexión a internet"
                        is OfferError.Rejected -> err.message // <- corregido de .reason a .message
                        is OfferError.NotABusiness -> "Solo los vecinos pueden validar ofertas"
                        is OfferError.Unknown -> err.message ?: "No pudimos confirmar la oferta"
                        else -> "No pudimos confirmar la oferta"
                    }
                    _state.update { it.copy(message = errorMsg) }
                }
            }
        }
    }

    fun toggleFavorite() {
        val offer = _state.value.offer ?: return
        if (favoriteInFlight) return
        favoriteInFlight = true

        val target = !offer.favorite
        setFavoriteLocally(target)
        favoritesSync.set(offer.id, target)

        viewModelScope.launch {
            repository.setFavorite(offer.id, target).onFailure { e ->
                setFavoriteLocally(!target)
                favoritesSync.set(offer.id, !target)
                _state.update { it.copy(message = e.message) }
            }
            favoriteInFlight = false
        }
    }

    private fun setFavoriteLocally(favorite: Boolean) {
        _state.update { s -> s.copy(offer = s.offer?.copy(favorite = favorite)) }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }


}