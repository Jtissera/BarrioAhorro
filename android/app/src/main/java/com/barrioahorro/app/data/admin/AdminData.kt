package com.barrioahorro.app.data.admin

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

data class AdminPublicationDto(
    val id: Long,
    /** CUPON | SORTEO */
    val kind: String,
    val businessName: String?,
    val businessAddress: String?,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val submittedAt: String?,
    val points: Int?,
    val stock: Int?,
    val amount: Double?,
)

data class RejectRequestDto(val motivo: String)

interface AdminApi {
    @GET("api/admin/publicaciones")
    suspend fun list(): List<AdminPublicationDto>

    @GET("api/admin/publicaciones/{kind}/{id}")
    suspend fun detail(@Path("kind") kind: String, @Path("id") id: Long): AdminPublicationDto

    @POST("api/admin/publicaciones/{kind}/{id}/aprobar")
    suspend fun approve(@Path("kind") kind: String, @Path("id") id: Long)

    @POST("api/admin/publicaciones/{kind}/{id}/rechazar")
    suspend fun reject(@Path("kind") kind: String, @Path("id") id: Long, @Body body: RejectRequestDto)
}

@Singleton
class AdminRepository @Inject constructor(private val api: AdminApi) {

    suspend fun pending() = call { api.list() }
    suspend fun detail(kind: String, id: Long) = call { api.detail(kind, id) }
    suspend fun approve(kind: String, id: Long) = call { api.approve(kind, id) }
    suspend fun reject(kind: String, id: Long, reason: String) =
        call { api.reject(kind, id, RejectRequestDto(reason)) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(Exception(when (e.code()) {
                400 -> "El motivo debe tener entre 15 y 300 caracteres."
                401 -> "Tu sesión venció. Volvé a iniciar sesión."
                403 -> "No tenés permiso para moderar."
                404 -> "Esta publicación ya fue revisada."
                else -> "Error del servidor (${e.code()}). Intentá de nuevo."
            }))
        } catch (e: IOException) {
            Result.failure(Exception("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado. Intentá de nuevo."))
        }
}

@Module
@InstallIn(SingletonComponent::class)
object AdminModule {
    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)
}