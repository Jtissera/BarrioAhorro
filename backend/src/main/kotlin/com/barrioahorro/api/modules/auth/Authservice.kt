package com.barrioahorro.api.modules.auth

import com.barrioahorro.api.core.exception.EmailAlreadyExistsException
import com.barrioahorro.api.core.exception.InvalidCredentialsException
import com.barrioahorro.api.core.exception.PasswordMismatchException
import com.barrioahorro.api.core.security.JwtService
import com.barrioahorro.api.modules.auth.dto.AuthResponse
import com.barrioahorro.api.modules.auth.dto.LoginRequest
import com.barrioahorro.api.modules.auth.dto.RegisterRequest
import com.barrioahorro.api.modules.business.BusinessEntity
import com.barrioahorro.api.modules.business.BusinessRepository
import com.barrioahorro.api.modules.user.UserEntity
import com.barrioahorro.api.modules.user.UserRepository
import com.barrioahorro.api.modules.user.enum.TipoUsuario
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val businessRepository: BusinessRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (!request.passwordsMatch) {
            throw PasswordMismatchException()
        }
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        val user = userRepository.save(
            UserEntity(
                email = request.email,
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)) {
                    "El encoder no debería devolver null"
                },
                tipoUsuario = request.tipoUsuario,
            ),
        )

        if (user.tipoUsuario == TipoUsuario.COMERCIO) {
            businessRepository.save(BusinessEntity(usuarioId = user.id))
        }

        return buildAuthResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return buildAuthResponse(user)
    }

    private fun buildAuthResponse(user: UserEntity): AuthResponse {
        val token = jwtService.generateToken(user.id, user.tipoUsuario.name)
        return AuthResponse(
            accessToken = token,
            userId = user.id,
            email = user.email,
            tipoUsuario = user.tipoUsuario,
        )
    }
}