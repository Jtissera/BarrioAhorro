package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.AuthResponseDto
import com.barrioahorro.app.data.remote.dto.LoginRequestDto
import com.barrioahorro.app.data.remote.dto.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>
}