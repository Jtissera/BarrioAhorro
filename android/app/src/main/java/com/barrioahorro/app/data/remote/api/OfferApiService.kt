package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.CreateOffer2x1RequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferCantidadRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorMayorRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorcentajeRequestDto
import com.barrioahorro.app.data.remote.dto.OfferCreatedDto
import com.barrioahorro.app.data.remote.dto.OfferPhotoDto
import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface OfferApiService {

    @POST("api/business/me/offers/porcentaje")
    suspend fun createPorcentaje(@Body request: CreateOfferPorcentajeRequestDto): Response<OfferCreatedDto>

    @POST("api/business/me/offers/cantidad")
    suspend fun createCantidad(@Body request: CreateOfferCantidadRequestDto): Response<OfferCreatedDto>

    @POST("api/business/me/offers/2x1")
    suspend fun create2x1(@Body request: CreateOffer2x1RequestDto): Response<OfferCreatedDto>

    @POST("api/business/me/offers/por-mayor")
    suspend fun createPorMayor(@Body request: CreateOfferPorMayorRequestDto): Response<OfferCreatedDto>

    @Multipart
    @POST("api/business/me/offers/fotos")
    suspend fun uploadPhoto(@Part file: MultipartBody.Part): Response<OfferPhotoDto>

    @GET("api/business/me/offers")
    suspend fun getMyOffers(): Response<List<OfferResponseDto>>

    @GET("api/business/me/offers/{id}")
    suspend fun getOffer(@Path("id") id: Long): Response<OfferResponseDto>

    @DELETE("api/business/me/offers/{id}")
    suspend fun deleteOffer(@Path("id") id: Long): Response<Unit>

    @PUT("api/business/me/offers/{id}/porcentaje")
    suspend fun updatePorcentaje(@Path("id") id: Long, @Body body: CreateOfferPorcentajeRequestDto): Response<OfferCreatedDto>

    @PUT("api/business/me/offers/{id}/cantidad")
    suspend fun updateCantidad(@Path("id") id: Long, @Body body: CreateOfferCantidadRequestDto): Response<OfferCreatedDto>

    @PUT("api/business/me/offers/{id}/2x1")
    suspend fun update2x1(@Path("id") id: Long, @Body body: CreateOffer2x1RequestDto): Response<OfferCreatedDto>

    @PUT("api/business/me/offers/{id}/por-mayor")
    suspend fun updatePorMayor(@Path("id") id: Long, @Body body: CreateOfferPorMayorRequestDto): Response<OfferCreatedDto>
}

