package com.barrioahorro.app.ui.screens.business

import com.barrioahorro.app.data.remote.dto.BusinessReviewDto
import com.barrioahorro.app.data.remote.dto.ReviewsSummaryDto
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

data class OpenStatus(val isOpen: Boolean, val text: String)

data class StarBarUi(val stars: Int, val count: Int, val fraction: Float)

data class ReviewsSummaryUi(
    val average: Double,
    val total: Int,
    val bars: List<StarBarUi>,
) {
    val averageText: String get() = formatAverage(average)
}

data class ProfileReviewUi(
    val id: Long,
    val authorName: String,
    val initials: String,
    val rating: Int,
    val comment: String,
    val relativeDate: String,
)

/** "Abierto ahora · Cierra 20:00" / "Cerrado ahora · Abre 16:30", based on today's slots. */
fun computeOpenStatus(schedule: List<DayScheduleUi>, now: LocalDateTime): OpenStatus {
    val today = schedule.firstOrNull { it.diaSemana == now.dayOfWeek.value }
    val ranges = if (today != null && today.enabled) {
        today.slots.mapNotNull { slot ->
            runCatching {
                LocalTime.parse(slot.horaInicio.take(5)) to LocalTime.parse(slot.horaFin.take(5))
            }.getOrNull()
        }
    } else {
        emptyList()
    }
    val time = now.toLocalTime()

    val current = ranges.firstOrNull { time >= it.first && time < it.second }
    if (current != null) {
        return OpenStatus(true, "Abierto ahora · Cierra ${current.second.format(HOUR_FORMAT)}")
    }
    val next = ranges.filter { it.first > time }.minByOrNull { it.first }
    return if (next != null) {
        OpenStatus(false, "Cerrado ahora · Abre ${next.first.format(HOUR_FORMAT)}")
    } else {
        OpenStatus(false, "Cerrado ahora")
    }
}

fun isOpenToday(schedule: List<DayScheduleUi>, diaSemana: Int): Boolean =
    schedule.any { it.diaSemana == diaSemana && it.enabled && it.slots.isNotEmpty() }

fun formatAverage(average: Double): String =
    String.format(Locale.forLanguageTag("es-AR"), "%.1f", average)

fun reviewsCountText(total: Int): String = if (total == 1) "1 reseña" else "$total reseñas"

fun ReviewsSummaryDto.toUi(): ReviewsSummaryUi = ReviewsSummaryUi(
    average = average,
    total = total,
    bars = distribution.sortedByDescending { it.stars }.map {
        StarBarUi(
            stars = it.stars,
            count = it.count,
            fraction = if (total == 0) 0f else it.count / total.toFloat(),
        )
    },
)

fun BusinessReviewDto.toUi(now: OffsetDateTime): ProfileReviewUi = ProfileReviewUi(
    id = id,
    authorName = authorName,
    initials = initialsOf(authorName),
    rating = rating.coerceIn(0, 5),
    comment = comment,
    relativeDate = formatRelativeDate(createdAt, now),
)

/** "martin.r" -> "MR", "pepe" -> "PE". */
fun initialsOf(name: String): String {
    val parts = name.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
        parts.size == 1 -> parts[0].take(2)
        else -> "?"
    }.uppercase()
}

/** "Hace 3 días", "Hace 1 semana"... Returns an empty string when the date cannot be parsed. */
fun formatRelativeDate(createdAt: String, now: OffsetDateTime): String {
    val created = runCatching { OffsetDateTime.parse(createdAt) }.getOrNull() ?: return ""
    val minutes = ChronoUnit.MINUTES.between(created, now).coerceAtLeast(0)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days < 7 -> "Hace $days ${plural(days, "día", "días")}"
        days < 30 -> (days / 7).let { "Hace $it ${plural(it, "semana", "semanas")}" }
        days < 365 -> (days / 30).let { "Hace $it ${plural(it, "mes", "meses")}" }
        else -> (days / 365).let { "Hace $it ${plural(it, "año", "años")}" }
    }
}

private fun plural(n: Long, one: String, many: String) = if (n == 1L) one else many