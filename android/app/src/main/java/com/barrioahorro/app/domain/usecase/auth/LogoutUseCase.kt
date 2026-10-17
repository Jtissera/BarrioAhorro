package com.barrioahorro.app.domain.usecase.auth

import com.barrioahorro.app.domain.repository.IAuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
) {
    suspend operator fun invoke() = authRepository.logout()
}