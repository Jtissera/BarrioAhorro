package com.barrioahorro.api.modules.explore

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/cliente")
class ClientProfileController(
    private val service: ClientProfileService,
) {

    @GetMapping("/perfil")
    fun profile(auth: Authentication): ClientProfileResponse = service.profile(userId(auth))

    @GetMapping("/resenas")
    fun myReviews(
        auth: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): MyReviewsPage = service.myReviews(userId(auth), page, size)

    private fun userId(auth: Authentication): Long =
        auth.name.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida")
}