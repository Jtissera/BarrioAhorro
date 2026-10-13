package com.barrioahorro.app.data.cuponera

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Los nombres de los campos coinciden 1 a 1 con el JSON del backend.
// Si usás kotlinx.serialization, agregá @Serializable a cada data class.

data class CuponeraProductRequestDto(
    val nombreProducto: String,
    val descripcion: String? = null,
    val fotoUrl: String? = null,
    val valorComercio: Double,
    val unidadesDisponibles: Int,
)

data class RatifyCuponeraRequestDto(
    val productos: List<CuponeraProductRequestDto>,
)

data class CuponeraProductDto(
    val id: Long,
    val nombreProducto: String,
    val descripcion: String?,
    val fotoUrl: String?,
    val valorComercio: Double?,
    val costoPuntos: Int,
    val unidadesDisponibles: Int,
    val unidadesCanjeadas: Int,
)

data class CuponeraCommitmentDto(
    val id: Long,
    val periodoInicio: String,
    val periodoFin: String,
    val fechaLimiteRatificacion: String,
    /** PENDIENTE | RATIFICADO | VENCIDO */
    val estado: String,
    val fechaRatificacion: String?,
    val puedeRatificar: Boolean,
    val minProductos: Int,
    val maxProductos: Int,
    val pesosPorPunto: Int,
    val productos: List<CuponeraProductDto>,
    val sugerencias: List<String>,
)

interface CuponeraApi {

    @GET("cuponera/aporte")
    suspend fun getCurrent(): CuponeraCommitmentDto

    @POST("cuponera/aporte/ratificar")
    suspend fun ratify(@Body body: RatifyCuponeraRequestDto): CuponeraCommitmentDto
}