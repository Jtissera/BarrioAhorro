package com.barrioahorro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val esAR: Locale = Locale.forLanguageTag("es-AR")

sealed interface OfferPriceInfo {
    data object Empty : OfferPriceInfo

    data class TwoForOne(
        val unitPrice: BigDecimal,
        val regularPrice: BigDecimal,
        val unitsToTake: Int,
        val unitsToPay: Int,
    ) : OfferPriceInfo

    data class Wholesale(
        val wholesalePrice: BigDecimal,
        val regularPrice: BigDecimal?,
    ) : OfferPriceInfo

    data class Quantity(
        val regularPrice: BigDecimal,
        val discountedPrice: BigDecimal,
        val requiredQuantity: Int,
    ) : OfferPriceInfo

    data class Percentage(
        val regularPrice: BigDecimal,
        val discountedPrice: BigDecimal,
    ) : OfferPriceInfo
}

@Composable
fun OfferPriceBlock(
    info: OfferPriceInfo,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        when (info) {
            OfferPriceInfo.Empty ->
                Text("$ —", style = MaterialTheme.typography.titleLarge, color = WarmGray)

            is OfferPriceInfo.Percentage -> {
                UnitPrice(info.discountedPrice)
                StrikeBefore(info.regularPrice)
            }

            is OfferPriceInfo.TwoForOne -> {
                UnitPrice(info.unitPrice)
                StrikeBefore(info.regularPrice)
                Detail("Llevando ${info.unitsToTake}, pagás ${info.unitsToPay}")
            }

            is OfferPriceInfo.Quantity -> {
                val n = info.requiredQuantity
                val average = info.regularPrice.multiply(BigDecimal(n - 1))
                    .add(info.discountedPrice)
                    .divide(BigDecimal(n), 0, RoundingMode.HALF_UP)
                UnitPrice(average)
                StrikeBefore(info.regularPrice)
                Detail("Llevando $n, la ${ordinal(n)} a ${formatMoney(info.discountedPrice)}")
            }

            is OfferPriceInfo.Wholesale -> {
                UnitPrice(info.wholesalePrice)
                info.regularPrice?.takeIf { it > info.wholesalePrice }?.let { StrikeBefore(it) }
            }
        }
    }
}

@Composable
private fun UnitPrice(price: BigDecimal) {
    Text(
        "${formatMoney(price)} c/u",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Ink,
    )
}

@Composable
private fun Detail(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = WarmGray,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun StrikeBefore(price: BigDecimal) {
    Text(
        buildAnnotatedString {
            append("antes ")
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(formatMoney(price))
            }
        },
        style = MaterialTheme.typography.bodySmall,
        color = WarmGray,
    )
}

fun formatMoney(value: BigDecimal): String {
    val nf = NumberFormat.getNumberInstance(esAR).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "$" + nf.format(value)
}

fun ordinal(n: Int): String = when (n) {
    1, 3 -> "${n}ra"
    2 -> "2da"
    4, 5, 6 -> "${n}ta"
    7, 10 -> "${n}ma"
    8 -> "8va"
    9 -> "9na"
    else -> "${n}ª"
}

object OfferPricing {
    fun percentageFinal(original: BigDecimal, percent: Int): BigDecimal =
        original.multiply(BigDecimal(100 - percent))
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

    fun twoForOneUnit(regular: BigDecimal, take: Int, pay: Int): BigDecimal? =
        if (take > 0) regular.multiply(BigDecimal(pay)).divide(BigDecimal(take), 2, RoundingMode.HALF_UP) else null

    /** % de ahorro del precio por mayor respecto del regular. Una sola fórmula para cartel y formulario. */
    fun wholesalePercent(regular: BigDecimal, wholesale: BigDecimal): Int? {
        if (regular <= BigDecimal.ZERO || wholesale <= BigDecimal.ZERO || wholesale >= regular) return null
        return regular.subtract(wholesale)
            .divide(regular, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }
}