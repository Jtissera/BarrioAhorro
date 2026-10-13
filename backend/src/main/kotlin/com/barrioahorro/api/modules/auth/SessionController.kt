package com.barrioahorro.api.modules.auth

import com.barrioahorro.api.core.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RefreshResponse(
    val accessToken: String,
    val tipoUsuario: String,
)


@RestController
@RequestMapping("/api/session")
class SessionController(
    private val jwtService: JwtService,
) {

    @PostMapping("/refresh")
    fun refresh(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<RefreshResponse> {
        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val userId = jwtService.extractUserId(token)
        val tipoUsuario = jwtService.extractTipoUsuario(token)

        return ResponseEntity.ok(
            RefreshResponse(
                accessToken = jwtService.generateToken(userId, tipoUsuario),
                tipoUsuario = tipoUsuario,
            ),
        )
    }
}