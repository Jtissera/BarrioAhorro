package com.barrioahorro.app.domain.usecase.auth

import com.barrioahorro.app.domain.model.AuthError
import com.barrioahorro.app.domain.model.AuthSession
import com.barrioahorro.app.domain.repository.IAuthRepository
import com.barrioahorro.app.domain.repository.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession, AuthError> =
        authRepository.login(email.trim(), password)
}