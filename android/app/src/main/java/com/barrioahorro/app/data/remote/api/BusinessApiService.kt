package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.BusinessResponseDto
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface BusinessApiService {

    @GET("api/business/me")
    suspend fun getMyBusiness(): Response<BusinessResponseDto>

    @PATCH("api/business/me")
    suspend fun updateMyBusiness(@Body request: UpdateBusinessRequestDto): Response<BusinessResponseDto>
}