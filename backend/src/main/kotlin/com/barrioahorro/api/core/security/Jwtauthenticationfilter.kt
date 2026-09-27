package com.barrioahorro.api.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val AUTH_HEADER = "Authorization"
private const val BEARER_PREFIX = "Bearer "

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null && jwtService.isTokenValid(token) && SecurityContextHolder.getContext().authentication == null) {
            val userId = jwtService.extractUserId(token)
            val tipoUsuario = jwtService.extractTipoUsuario(token)

            val authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_$tipoUsuario")),
            )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader(AUTH_HEADER) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.removePrefix(BEARER_PREFIX) else null
    }
}