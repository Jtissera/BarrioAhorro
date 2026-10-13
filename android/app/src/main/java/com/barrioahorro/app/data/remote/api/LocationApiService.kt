package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.ValidateAddressRequestDto
import com.barrioahorro.app.data.remote.dto.ValidateAddressResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApiService {

    @POST("api/location/validate")
    suspend fun validateAddress(@Body request: ValidateAddressRequestDto): Response<ValidateAddressResponseDto>
}