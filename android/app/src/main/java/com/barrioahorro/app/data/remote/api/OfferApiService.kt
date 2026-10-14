package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.CreateOfferCantidadRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorcentajeRequestDto
import com.barrioahorro.app.data.remote.dto.OfferCreatedDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OfferApiService {

    @POST("api/business/me/offers/porcentaje")
    suspend fun createPorcentaje(@Body request: CreateOfferPorcentajeRequestDto): Response<OfferCreatedDto>

    @POST("api/business/me/offers/cantidad")
    suspend fun createCantidad(@Body request: CreateOfferCantidadRequestDto): Response<OfferCreatedDto>
}

