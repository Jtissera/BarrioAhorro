package com.barrioahorro.app.ui.screens.business

import com.barrioahorro.app.data.remote.dto.PhotoResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BusinessPhotoEditingTest {

    private val base = "http://10.0.2.2:8080/"

    @Test
    fun `resolvePhotoUrl arma la url completa a partir de la ruta del backend`() {
        assertEquals("http://10.0.2.2:8080/uploads/a.jpg", resolvePhotoUrl(base, "/uploads/a.jpg"))
    }

    @Test
    fun `resolvePhotoUrl deja igual una url que ya es absoluta`() {
        assertEquals("https://cdn.ejemplo.com/a.jpg", resolvePhotoUrl(base, "https://cdn.ejemplo.com/a.jpg"))
    }

    @Test
    fun `toPhotoUi ordena por el campo orden`() {
        val photos = listOf(
            PhotoResponseDto(id = 7, url = "/uploads/b.jpg", orden = 1),
            PhotoResponseDto(id = 3, url = "/uploads/a.jpg", orden = 0),
        )

        val ui = photos.toPhotoUi(base)

        assertEquals(listOf(3L, 7L), ui.map { it.id })
        assertEquals("http://10.0.2.2:8080/uploads/a.jpg", ui[0].url)
    }

    @Test
    fun `moved mueve una foto un lugar hacia adelante`() {
        val photos = listOf(PhotoUi(1, "a"), PhotoUi(2, "b"), PhotoUi(3, "c"))

        assertEquals(listOf(2L, 1L, 3L), photos.moved(photoId = 2, offset = -1)?.map { it.id })
    }

    @Test
    fun `moved mueve una foto un lugar hacia atras`() {
        val photos = listOf(PhotoUi(1, "a"), PhotoUi(2, "b"), PhotoUi(3, "c"))

        assertEquals(listOf(1L, 3L, 2L), photos.moved(photoId = 2, offset = 1)?.map { it.id })
    }

    @Test
    fun `moved devuelve null si la foto ya esta en el borde`() {
        val photos = listOf(PhotoUi(1, "a"), PhotoUi(2, "b"))

        assertNull(photos.moved(photoId = 1, offset = -1))
        assertNull(photos.moved(photoId = 2, offset = 1))
    }

    @Test
    fun `moved devuelve null si la foto no existe`() {
        assertNull(listOf(PhotoUi(1, "a")).moved(photoId = 99, offset = 1))
    }
}
