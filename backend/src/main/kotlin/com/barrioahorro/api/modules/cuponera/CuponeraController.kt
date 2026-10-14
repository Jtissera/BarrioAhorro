package com.barrioahorro.api.modules.cuponera

import com.barrioahorro.api.modules.cuponera.dto.CuponeraCommitmentResponse
import com.barrioahorro.api.modules.cuponera.dto.CuponeraPhotoUploadResponse
import com.barrioahorro.api.modules.cuponera.dto.RatifyCuponeraRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/cuponera/aporte")
class CuponeraController(
    private val service: CuponeraService,
) {

    @GetMapping
    fun current(auth: Authentication): CuponeraCommitmentResponse =
        service.getCurrent(userId(auth))

    @PostMapping("/ratificar")
    fun ratify(
        auth: Authentication,
        @Valid @RequestBody body: RatifyCuponeraRequest,
    ): CuponeraCommitmentResponse = service.ratify(userId(auth), body)

    @PostMapping("/fotos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(
        auth: Authentication,
        @RequestParam("file") file: MultipartFile,
    ): CuponeraPhotoUploadResponse = service.uploadPhoto(userId(auth), file)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}