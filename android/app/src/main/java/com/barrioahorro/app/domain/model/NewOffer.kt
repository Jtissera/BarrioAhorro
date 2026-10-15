package com.barrioahorro.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

sealed class NewOffer {
    abstract val productName: String
    abstract val validUntil: LocalDateTime
    abstract val photoUrl: String?

    data class Percentage(
        override val productName: String,
        override val validUntil: LocalDateTime,
        val originalPrice: BigDecimal,
        val discountPercent: Int,
        override val photoUrl: String? = null,
    ) : NewOffer()

    data class LastUnit(
        override val productName: String,
        override val validUntil: LocalDateTime,
        val unitPrice: BigDecimal,
        val requiredQuantity: Int,
        val lastUnitDiscountPercent: Int,
        override val photoUrl: String? = null,
    ) : NewOffer()
}

