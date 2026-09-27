package com.barrioahorro.api.core.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtService(
    @Value("\${app.security.jwt.secret}") secret: String,
    @Value("\${app.security.jwt.expiration-minutes:1440}") private val expirationMinutes: Long,
) {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: Long, tipoUsuario: String): String {
        val now = Date()
        val expiration = Date(now.time + expirationMinutes * 60_000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("tipoUsuario", tipoUsuario)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact()
    }

    fun extractUserId(token: String): Long = parseClaims(token).subject.toLong()

    fun extractTipoUsuario(token: String): String = parseClaims(token)["tipoUsuario", String::class.java]

    fun isTokenValid(token: String): Boolean =
        try {
            parseClaims(token).expiration.after(Date())
        } catch (ex: Exception) {
            println("JWT inválido: ${ex.javaClass.simpleName} - ${ex.message}")
            false
        }

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}