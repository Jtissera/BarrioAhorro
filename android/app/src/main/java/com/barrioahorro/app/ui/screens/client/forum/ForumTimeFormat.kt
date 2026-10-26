package com.barrioahorro.app.ui.screens.client.forum

import java.time.Duration
import java.time.OffsetDateTime

/** Fecha relativa compacta ("hace 2 d") compartida por las pantallas del foro. */
internal fun formatRelativeTime(dateString: String): String {
    return try {
        val parsed = OffsetDateTime.parse(dateString)
        val now = OffsetDateTime.now()
        val duration = Duration.between(parsed, now)
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()
        when {
            duration.isNegative || minutes < 1 -> "recién"
            minutes < 60 -> "hace $minutes min"
            hours < 24 -> "hace $hours h"
            days < 7 -> "hace $days d"
            else -> "hace ${days / 7} sem"
        }
    } catch (e: Exception) {
        "reciente"
    }
}
