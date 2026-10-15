package com.barrioahorro.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

sealed class NewOffer {
    abstract val productName: String
    abstract val vigenciaTipo: String
    abstract val validUntil: LocalDateTime?
    abstract val photoUrl: String?

    data class Percentage(
        override val productName: String,
        override val vigenciaTipo: String = "FECHA_ESPECIFICA",
        override val validUntil: LocalDateTime? = null,
        val originalPrice: BigDecimal,
        val discountPercent: Int,
        override val photoUrl: String? = null,
    ) : NewOffer()

    data class LastUnit(
        override val productName: String,
        override val vigenciaTipo: String = "FECHA_ESPECIFICA",
        override val validUntil: LocalDateTime? = null,
        val unitPrice: BigDecimal,
        val requiredQuantity: Int,
        val lastUnitDiscountPercent: Int,
        override val photoUrl: String? = null,
    ) : NewOffer()

    data class TwoForOne(
        override val productName: String,
        val unitsToTake: Short,
        val unitsToPay: Short,
        val unitPrice: BigDecimal,
        override val vigenciaTipo: String = "HASTA_AGOTAR_STOCK",
        override val validUntil: LocalDateTime? = null,
        override val photoUrl: String? = null,
    ) : NewOffer()
}

