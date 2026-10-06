package com.barrioahorro.app.data.remote.api

import com.barrioahorro.app.data.remote.dto.BusinessResponseDto
import com.barrioahorro.app.data.remote.dto.PhotoResponseDto
import com.barrioahorro.app.data.remote.dto.ReorderPhotosRequestDto
import com.barrioahorro.app.data.remote.dto.UpdateBusinessRequestDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface BusinessApiService {

    @GET("api/business/me")
    suspend fun getMyBusiness(): Response<BusinessResponseDto>

    @PATCH("api/business/me")
    suspend fun updateMyBusiness(@Body request: UpdateBusinessRequestDto): Response<BusinessResponseDto>

    @Multipart
    @POST("api/business/me/photos")
    suspend fun addPhoto(@Part file: MultipartBody.Part): Response<PhotoResponseDto>

    @Multipart
    @PUT("api/business/me/photos/{photoId}")
    suspend fun replacePhoto(@Path("photoId") photoId: Long, @Part file: MultipartBody.Part): Response<PhotoResponseDto>

    @DELETE("api/business/me/photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: Long): Response<Unit>

    @PUT("api/business/me/photos/order")
    suspend fun reorderPhotos(@Body request: ReorderPhotosRequestDto): Response<List<PhotoResponseDto>>
}
