package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.PhotoResponse
import com.barrioahorro.api.modules.business.dto.ReorderPhotosRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

const val MAX_PHOTOS_PER_BUSINESS = 5

@RestController
@RequestMapping("/api/business/me/photos")
class BusinessPhotoController(
    private val photoRepository: BusinessPhotoRepository,
    private val photoStorage: PhotoStorage,
) {

    @GetMapping
    fun listMyPhotos(authentication: Authentication): List<PhotoResponse> =
        photoRepository.findByComercioIdOrderByOrdenAscIdAsc(authentication.userId()).map { it.toResponse() }

    @Transactional
    @PostMapping
    fun addPhoto(
        authentication: Authentication,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<PhotoResponse> {
        val userId = authentication.userId()
        val count = photoRepository.countByComercioId(userId)
        if (count >= MAX_PHOTOS_PER_BUSINESS) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Podés tener hasta $MAX_PHOTOS_PER_BUSINESS fotos")
        }

        val photo = photoRepository.save(
            BusinessPhotoEntity(comercioId = userId, url = storeImage(file), orden = count.toShort()),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(photo.toResponse())
    }

    @Transactional
    @PutMapping("/{photoId}")
    fun replacePhoto(
        authentication: Authentication,
        @PathVariable photoId: Long,
        @RequestParam("file") file: MultipartFile,
    ): PhotoResponse {
        val photo = findOwnPhoto(authentication, photoId)
        val oldUrl = photo.url

        photo.url = storeImage(file)
        val saved = photoRepository.saveAndFlush(photo)
        photoStorage.delete(oldUrl)
        return saved.toResponse()
    }

    @Transactional
    @DeleteMapping("/{photoId}")
    fun deletePhoto(authentication: Authentication, @PathVariable photoId: Long): ResponseEntity<Void> {
        val photo = findOwnPhoto(authentication, photoId)

        photoRepository.delete(photo)
        photoRepository.flush()
        // Reacomodamos el orden para que no queden huecos.
        photoRepository.findByComercioIdOrderByOrdenAscIdAsc(photo.comercioId)
            .forEachIndexed { index, remaining -> remaining.orden = index.toShort() }
        photoStorage.delete(photo.url)

        return ResponseEntity.noContent().build()
    }

    @Transactional
    @PutMapping("/order")
    fun reorderPhotos(
        authentication: Authentication,
        @RequestBody request: ReorderPhotosRequest,
    ): List<PhotoResponse> {
        val photos = photoRepository.findByComercioIdOrderByOrdenAscIdAsc(authentication.userId())
        val byId = photos.associateBy { it.id }

        if (request.photoIds.size != photos.size || request.photoIds.toSet() != byId.keys) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El nuevo orden debe incluir todas tus fotos, una vez cada una")
        }

        return request.photoIds.mapIndexed { index, id ->
            byId.getValue(id).also { it.orden = index.toShort() }
        }.map { it.toResponse() }
    }

    private fun storeImage(file: MultipartFile): String =
        photoStorage.store(file.bytes)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser una imagen JPG, PNG o WebP")

    private fun findOwnPhoto(authentication: Authentication, photoId: Long): BusinessPhotoEntity =
        photoRepository.findByIdAndComercioId(photoId, authentication.userId())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no encontrada")

    private fun Authentication.userId(): Long = principal as Long
}

fun BusinessPhotoEntity.toResponse() = PhotoResponse(id = id, url = url, orden = orden.toInt())
