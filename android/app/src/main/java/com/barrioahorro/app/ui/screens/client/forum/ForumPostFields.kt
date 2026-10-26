package com.barrioahorro.app.ui.screens.client.forum

import com.barrioahorro.app.data.forum.ForumPostDto

/** Límites que también valida el backend (CreateForumPostRequest / UpdateCommentRequest). */
const val FORUM_POST_MAX_LENGTH = 300
const val FORUM_COMMENT_MAX_LENGTH = 500

/**
 * Convierte lo que escribió el vecino ("$ 1.200", "1500,50") en un precio.
 * El punto se toma como separador de miles y la coma como decimal.
 */
fun parseForumPrice(input: String): Double? {
    if (input.isBlank()) return null
    val cleaned = input
        .replace("$", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
    return cleaned.toDoubleOrNull()?.takeIf { it > 0 }
}

/** Inverso de [parseForumPrice] para precargar el campo: 1500.5 -> "1500,5". */
fun formatForumPriceInput(price: Double?): String {
    if (price == null) return ""
    return if (price % 1.0 == 0.0) {
        price.toLong().toString()
    } else {
        price.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
    }
}

/** Solo dígitos y separadores en el campo de precio. */
fun sanitizePriceInput(input: String): String = input.filter { it.isDigit() || it == '.' || it == ',' }

/**
 * Texto del chip de ubicación: la dirección que escribió el autor o,
 * si no hay pero sí hay punto, el comercio vinculado. Null si no hay ubicación.
 */
fun ForumPostDto.locationLabel(): String? {
    direccion?.takeIf { it.isNotBlank() }?.let { return it }
    return if (latitud != null && longitud != null) "Visto en $comercioNombre" else null
}
