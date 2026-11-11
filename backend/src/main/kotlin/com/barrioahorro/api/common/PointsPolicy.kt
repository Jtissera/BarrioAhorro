package com.barrioahorro.api.common

import java.math.BigDecimal
import java.math.RoundingMode

object PointsPolicy {
    const val PESOS_PER_POINT = 1000

    /** Redondea hacia arriba, mínimo 1. */
    fun pointsFor(value: BigDecimal): Int =
        value.divide(BigDecimal(PESOS_PER_POINT), 0, RoundingMode.CEILING)
            .toInt()
            .coerceAtLeast(1)
}