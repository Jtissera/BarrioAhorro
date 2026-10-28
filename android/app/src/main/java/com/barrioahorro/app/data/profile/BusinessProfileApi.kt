package com.barrioahorro.app.data.profile

import com.barrioahorro.app.data.explore.ExploreOfferDto
import com.barrioahorro.app.data.remote.dto.BusinessReviewDto
import com.barrioahorro.app.data.remote.dto.ReviewsSummaryDto
import com.barrioahorro.app.data.remote.dto.ScheduleSlotResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class BusinessProfileDto(
    val id: Long,
    val name: String?,
    val category: String?,
    val address: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceMeters: Int?,
    val photoUrl: String?,
    val schedule: List<ScheduleSlotResponseDto>,
)

data class BusinessReviewsPageDto(
    val summary: ReviewsSummaryDto,
    /** Reseña del propio vecino: no viene repetida en [items]. */
    val mine: BusinessReviewDto?,
    val items: List<BusinessReviewDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class WriteReviewRequestDto(
    val rating: Int,
    val comment: String,
)

interface BusinessProfileApi {

    @GET("api/comercios/{id}")
    suspend fun getProfile(
        @Path("id") businessId: Long,
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?,
    ): BusinessProfileDto

    @GET("api/comercios/{id}/ofertas")
    suspend fun getOffers(
        @Path("id") businessId: Long,
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?,
    ): List<ExploreOfferDto>

    @GET("api/comercios/{id}/resenas")
    suspend fun getReviews(
        @Path("id") businessId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): BusinessReviewsPageDto

    @PUT("api/comercios/{id}/resena")
    suspend fun saveReview(
        @Path("id") businessId: Long,
        @Body body: WriteReviewRequestDto,
    ): BusinessReviewDto

    @DELETE("api/comercios/{id}/resena")
    suspend fun deleteReview(@Path("id") businessId: Long): Response<Unit>
}
