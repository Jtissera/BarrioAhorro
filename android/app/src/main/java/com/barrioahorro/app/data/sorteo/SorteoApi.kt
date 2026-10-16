package com.barrioahorro.app.data.sorteo

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class SorteoCommitmentDto(
    val id: Long,
    val cicloInicio: String,
    val cicloFin: String,
    val fechaLimiteRatificacion: String,
    val estado: String,
    val fechaRatificacion: String?,
    val premioDescripcion: String?,
    val puedeRatificar: Boolean,
)

data class RatifySorteoRequestDto(
    val premioDescripcion: String,
)

interface SorteoApi {

    @GET("sorteo/compromiso")
    suspend fun getCurrent(): SorteoCommitmentDto

    @POST("sorteo/compromiso/ratificar")
    suspend fun ratify(@Body body: RatifySorteoRequestDto): SorteoCommitmentDto
}

