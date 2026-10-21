package com.barrioahorro.app.ui.screens.client.explore

import com.barrioahorro.app.data.explore.ExploreOfferDto
import com.barrioahorro.app.data.remote.dto.Offer2x1DetailResponseDto
import com.barrioahorro.app.data.remote.dto.OfferCantidadDetailResponseDto
import com.barrioahorro.app.data.remote.dto.OfferPorMayorDetailResponseDto
import com.barrioahorro.app.data.remote.dto.OfferPorcentajeDetailResponseDto
import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import com.barrioahorro.app.ui.screens.offers.MyOfferItemUi
import com.barrioahorro.app.ui.screens.offers.OfferUiMapper
import java.util.Locale

data class ExploreOfferUi(
    val id: Long,
    val businessId: Long,
    val businessOpen: Boolean,
    val favorite: Boolean,
    val card: MyOfferItemUi,
)

object ExploreOfferMapper {

    private val esAR = Locale.forLanguageTag("es-AR")

    fun toUi(d: ExploreOfferDto): ExploreOfferUi {
        val dto = OfferResponseDto(
            id = d.id,
            comercioId = d.businessId,
            nombreProducto = d.productName,
            fotoUrl = d.photoUrl,
            tipoOferta = d.type,
            vigenciaTipo = d.validityType,
            vigenteHasta = d.validUntil,
            detalle2x1 = if (d.type == "DOS_POR_UNO" && d.take != null && d.pay != null) {
                Offer2x1DetailResponseDto(d.pay.toShort(), d.take.toShort(), d.price)
            } else null,
            detallePorMayor = if (d.type == "POR_MAYOR" && d.minQuantity != null && d.price != null) {
                OfferPorMayorDetailResponseDto(d.minQuantity.toShort(), d.price, d.originalPrice)
            } else null,
            detalleCantidad = if (d.type == "CANTIDAD_ULTIMA_UNIDAD" &&
                d.requiredQuantity != null && d.lastUnitDiscount != null && d.price != null
            ) {
                OfferCantidadDetailResponseDto(d.requiredQuantity.toShort(), d.price, d.lastUnitDiscount.toShort())
            } else null,
            detallePorcentaje = if (d.type == "PORCENTAJE" && d.percent != null &&
                d.originalPrice != null && d.price != null
            ) {
                OfferPorcentajeDetailResponseDto(d.originalPrice, d.percent.toShort(), d.price)
            } else null,
        )

        val base = OfferUiMapper.toUi(dto, d.businessName.orEmpty(), d.category.orEmpty(), d.confirmations)

        val vigencia = if (d.validityType == "SOLO_HOY" && d.closesToday != null) {
            "Hasta hoy ${d.closesToday}"
        } else {
            base.vigenciaText
        }

        return ExploreOfferUi(
            id = d.id,
            businessId = d.businessId,
            businessOpen = d.businessOpen,
            favorite = d.favorite,
            card = base.copy(
                businessName = d.businessName ?: "Comercio",
                categoryName = d.category.orEmpty(),
                distanceText = formatDistance(d.distanceMeters),
                vigenciaText = vigencia,
                confirmedCountText = confirmations(d.confirmations),
            ),
        )
    }

    private fun confirmations(count: Int): String = when (count) {
        0 -> "Sin confirmaciones aún"
        1 -> "1 vecino confirma"
        else -> "$count vecinos confirman"
    }

    fun formatDistance(meters: Int?): String = when {
        meters == null -> ""
        meters < 1000 -> "a ${maxOf(10, (meters + 5) / 10 * 10)} m"
        else -> String.format(esAR, "a %.1f km", meters / 1000.0)
    }
}