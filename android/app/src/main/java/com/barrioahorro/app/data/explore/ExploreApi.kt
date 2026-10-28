package com.barrioahorro.app.data.explore

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExploreApi {

    @GET("api/cliente/resumen")
    suspend fun getSummary(): ClientSummaryDto

    @GET("api/explorar/ofertas")
    suspend fun getOffers(
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?,
        @Query("radioKm") radiusKm: Double?,
        @Query("rubros") categoryIds: List<Int>?,
        @Query("tipos") types: List<String>?,
        @Query("q") text: String?,
        @Query("orden") order: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ExploreOffersPageDto

    @GET("api/favoritos")
    suspend fun getFavorites(
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ExploreOffersPageDto

    @PUT("api/favoritos/{id}")
    suspend fun addFavorite(@Path("id") offerId: Long): Response<Unit>

    @DELETE("api/favoritos/{id}")
    suspend fun removeFavorite(@Path("id") offerId: Long): Response<Unit>

    @GET("api/explorar/comercios")
    suspend fun getBusinesses(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radioKm") radiusKm: Double?,
        @Query("rubros") categoryIds: List<Int>?,
        @Query("tipos") types: List<String>?,
        @Query("q") text: String?,
    ): List<ExploreBusinessDto>
}