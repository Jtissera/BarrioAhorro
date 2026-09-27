package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.domain.model.AuthError
import com.barrioahorro.app.domain.model.AuthSession
import com.barrioahorro.app.domain.model.UserType

interface IAuthRepository {

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        userType: UserType,
    ): Result<AuthSession, AuthError>

    suspend fun login(email: String, password: String): Result<AuthSession, AuthError>

    suspend fun hasActiveSession(): Boolean
}

sealed class Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
    data class Failure<out E>(val error: E) : Result<Nothing, E>()
}