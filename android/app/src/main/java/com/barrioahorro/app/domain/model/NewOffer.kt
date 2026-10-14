package com.barrioahorro.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

sealed class NewOffer {
    abstract val productName: String
    abstract val validUntil: LocalDateTime

    data class Percentage(
        override val productName: String,
        override val validUntil: LocalDateTime,
        val originalPrice: BigDecimal,
        val discountPercent: Int,
    ) : NewOffer()

    data class LastUnit(
        override val productName: String,
        override val validUntil: LocalDateTime,
        val unitPrice: BigDecimal,
        val requiredQuantity: Int,
        val lastUnitDiscountPercent: Int,
    ) : NewOffer()
}

