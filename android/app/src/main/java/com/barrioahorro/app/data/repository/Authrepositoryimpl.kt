package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.local.datastore.AuthTokenDataStore
import com.barrioahorro.app.data.remote.api.AuthApiService
import com.barrioahorro.app.data.remote.dto.AuthResponseDto
import com.barrioahorro.app.data.remote.dto.LoginRequestDto
import com.barrioahorro.app.data.remote.dto.RegisterRequestDto
import com.barrioahorro.app.domain.model.AuthError
import com.barrioahorro.app.domain.model.AuthSession
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.domain.repository.IAuthRepository
import com.barrioahorro.app.domain.repository.Result
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_CONFLICT = 409

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val authTokenDataStore: AuthTokenDataStore,
) : IAuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        userType: UserType,
    ): Result<AuthSession, AuthError> {
        val request = RegisterRequestDto(email, password, confirmPassword, userType.name)
        return runCatching { authApiService.register(request) }
            .fold(
                onSuccess = { response -> response.toSessionResult() },
                onFailure = { it.toAuthResult() },
            )
    }

    override suspend fun login(email: String, password: String): Result<AuthSession, AuthError> {
        val request = LoginRequestDto(email, password)
        return runCatching { authApiService.login(request) }
            .fold(
                onSuccess = { response -> response.toSessionResult() },
                onFailure = { it.toAuthResult() },
            )
    }

    override suspend fun hasActiveSession(): Boolean =
        authTokenDataStore.currentToken() != null

    private suspend fun Response<AuthResponseDto>.toSessionResult(): Result<AuthSession, AuthError> {
        val body = body()
        if (!isSuccessful || body == null) {
            return Result.Failure(mapHttpError(code()))
        }

        authTokenDataStore.saveSession(body.accessToken, body.tipoUsuario)

        return Result.Success(
            AuthSession(
                userId = body.userId,
                email = body.email,
                userType = UserType.valueOf(body.tipoUsuario),
                accessToken = body.accessToken,
            ),
        )
    }

    private fun mapHttpError(httpCode: Int): AuthError = when (httpCode) {
        HTTP_UNAUTHORIZED -> AuthError.InvalidCredentials
        HTTP_CONFLICT -> AuthError.EmailAlreadyExists
        else -> AuthError.Unknown(message = "Error del servidor ($httpCode)")
    }

    private fun Throwable.toAuthResult(): Result<AuthSession, AuthError> =
        Result.Failure(
            when (this) {
                is IOException -> AuthError.NoConnection
                else -> AuthError.Unknown(message = message)
            },
        )
}