package com.barrioahorro.app.ui.screens.client.explore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Cambios de favorito hechos en cualquier pantalla, para que Explorar y Favoritos no se desfasen. */
@Singleton
class FavoritesSync @Inject constructor() {
    private val _changes = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val changes: StateFlow<Map<Long, Boolean>> = _changes.asStateFlow()

    fun set(offerId: Long, favorite: Boolean) = _changes.update { it + (offerId to favorite) }
}