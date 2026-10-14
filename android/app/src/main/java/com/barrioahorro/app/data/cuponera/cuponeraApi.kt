package com.barrioahorro.app.data.cuponera

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Los nombres de los campos coinciden 1 a 1 con el JSON del backend.
// Si usás kotlinx.serialization, agregá @Serializable a cada data class.

@Serializable
data class CuponeraProductRequestDto(
    val nombreProducto: String,
    val descripcion: String? = null,
    val fotoUrl: String? = null,
    val valorComercio: Double,
    val unidadesDisponibles: Int,
)

@Serializable
data class RatifyCuponeraRequestDto(
    val productos: List<CuponeraProductRequestDto>,
)

@Serializable
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

@Serializable
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

@Serializable
interface CuponeraApi {

    @GET("cuponera/aporte")
    suspend fun getCurrent(): CuponeraCommitmentDto

    @POST("cuponera/aporte/ratificar")
    suspend fun ratify(@Body body: RatifyCuponeraRequestDto): CuponeraCommitmentDto
}