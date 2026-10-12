package com.barrioahorro.app.domain.usecase.auth

import com.barrioahorro.app.domain.model.AuthError
import com.barrioahorro.app.domain.model.AuthSession
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.domain.repository.IAuthRepository
import com.barrioahorro.app.domain.repository.Result
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        userType: UserType,
    ): Result<AuthSession, AuthError> {

        return authRepository.register(email.trim(), password, userType)
    }
}