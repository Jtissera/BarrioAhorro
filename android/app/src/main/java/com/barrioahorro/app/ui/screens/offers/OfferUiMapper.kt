package com.barrioahorro.app.ui.screens.offers

import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Maps an offer from the API to the card model shared by "Mis ofertas" and the merchant profile. */
object OfferUiMapper {

    private val esAR = Locale.forLanguageTag("es-AR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun toUi(
        dto: OfferResponseDto,
        businessName: String,
        categoryName: String,
        confirmationsCount: Int = 0,
    ): MyOfferItemUi {
        val tipo = dto.tipoOferta.uppercase()

        var badge = "PROMO"
        var isYellow = false
        var price = "$ —"
        var unit: String? = null
        var original: String? = null

        when {
            tipo.contains("PORCENTA") -> {
                val p = dto.detallePorcentaje
                badge = if (p != null) "-${p.porcentajeDescuento}%" else "-%"
                val finalP = p?.precioFinal ?: p?.precioOriginal
                if (finalP != null) price = formatMoney(finalP)
                if (p?.precioFinal != null) original = formatMoney(p.precioOriginal)
            }
            tipo.contains("2X1") || tipo.contains("DOS") -> {
                val d = dto.detalle2x1
                badge = if (d != null) "${d.unidadesALlevar}X${d.unidadesAPagar}" else "2X1"
                if (d?.precioUnitario != null) {
                    price = formatMoney(d.precioUnitario)
                    unit = "c/u"
                }
            }
            tipo.contains("MAYOR") -> {
                val m = dto.detallePorMayor
                badge = if (m != null) "Desde ${m.cantidadMinima} u." else "X MAYOR"
                isYellow = true
                if (m?.precioUnitarioMayorista != null) {
                    price = formatMoney(m.precioUnitarioMayorista)
                    unit = "c/u"
                }
                if (m?.precioUnitarioRegular != null) original = formatMoney(m.precioUnitarioRegular)
            }
            tipo.contains("CANTIDAD") -> {
                val c = dto.detalleCantidad
                badge = if (c != null) "${ordinal(c.cantidadRequerida)} AL ${c.porcentajeDescuentoUltimaUnidad}%" else "PROMO"
                if (c?.precioUnitario != null) {
                    price = formatMoney(c.precioUnitario)
                }
            }
        }

        return MyOfferItemUi(
            id = dto.id,
            productName = dto.nombreProducto,
            photoUrl = dto.fotoUrl,
            badgeText = badge,
            isYellowBadge = isYellow,
            priceMain = price,
            priceUnit = unit,
            priceOriginal = original,
            businessName = businessName.ifBlank { "Mi Comercio" },
            categoryName = categoryName.ifBlank { "Comercio" },
            vigenciaText = formatVigencia(dto.vigenciaTipo, dto.vigenteHasta),
            confirmedCountText = confirmationsText(confirmationsCount),
            isVigente = dto.vigente,
            isActiva = dto.activa,
        )
    }

    private fun confirmationsText(count: Int): String =
        if (count == 1) "1 vecino confirmó" else "$count vecinos confirmaron"

    private fun formatVigencia(vigenciaTipo: String, vigenteHasta: String?): String {
        return when (vigenciaTipo.uppercase()) {
            "HASTA_AGOTAR_STOCK" -> "Hasta agotar stock"
            "SOLO_HOY" -> "Hasta hoy 23:59"
            "TODA_LA_SEMANA" -> "Toda la semana"
            "FECHA_ESPECIFICA" -> {
                if (vigenteHasta == null) return "Fecha a definir"
                runCatching {
                    val local = runCatching {
                        OffsetDateTime.parse(vigenteHasta).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    }.getOrNull() ?: runCatching {
                        Instant.parse(vigenteHasta).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }.getOrNull() ?: LocalDateTime.parse(vigenteHasta)

                    val date = local.toLocalDate()
                    val today = LocalDate.now()
                    val time = local.format(timeFormatter)
                    when {
                        date == today -> "Hasta hoy $time"
                        date == today.plusDays(1) -> "Hasta mañana $time"
                        date <= today.plusDays(6) -> {
                            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, esAR)
                                .replaceFirstChar { it.lowercase() }
                            "Hasta el $dayName"
                        }
                        else -> "Hasta el ${date.format(DateTimeFormatter.ofPattern("dd/MM"))}"
                    }
                }.getOrDefault("Fecha a definir")
            }
            else -> "Hasta agotar stock"
        }
    }

    private fun formatMoney(value: BigDecimal): String {
        val nf = NumberFormat.getNumberInstance(esAR).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "$ " + nf.format(value)
    }

    private fun ordinal(n: Short): String = when (n.toInt()) {
        1, 3 -> "${n}RA"
        2 -> "2DA"
        4, 5, 6 -> "${n}TA"
        7, 10 -> "${n}MA"
        8 -> "8VA"
        9 -> "9NA"
        else -> "${n}ª"
    }
}