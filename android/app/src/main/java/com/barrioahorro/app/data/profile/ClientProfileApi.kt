package com.barrioahorro.app.data.profile

import retrofit2.http.GET
import retrofit2.http.Query

data class ClientProfileDto(
    val name: String,
    val points: Int,
    val postsCount: Int,
    val reviewsCount: Int,
)

data class MyReviewDto(
    val id: Long,
    val businessId: Long,
    val businessName: String?,
    val category: String?,
    val rating: Int,
    val comment: String,
    val createdAt: String,
)

data class MyReviewsPageDto(
    val items: List<MyReviewDto>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

interface ClientProfileApi {

    @GET("api/cliente/perfil")
    suspend fun getProfile(): ClientProfileDto

    @GET("api/cliente/resenas")
    suspend fun getMyReviews(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): MyReviewsPageDto
}