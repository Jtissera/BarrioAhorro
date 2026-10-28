package com.barrioahorro.app.data.cuponera

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Error con mensaje listo para mostrarle al usuario. */
class CuponeraException(message: String) : Exception(message)

@Singleton
class CuponeraRepository @Inject constructor(
    private val api: CuponeraApi,
) {

    suspend fun getCurrent(): Result<CuponeraCommitmentDto> = call { api.getCurrent() }

    suspend fun ratify(body: RatifyCuponeraRequestDto): Result<CuponeraCommitmentDto> =
        call { api.ratify(body) }

    suspend fun getOverview(): Result<CommitmentOverviewDto> = call { api.getOverview() }

    suspend fun ratifyRaffle(name: String, description: String, value: Double, photoUrl: String?): Result<RaffleCycleDto> =
        call { api.ratifyRaffle(RatifyRaffleRequestDto(name, description, value, photoUrl)) }

    suspend fun validateRedemption(code: String): Result<ScanRedemptionDto> =
        call { api.validateRedemption(ScanRedemptionRequestDto(code)) }

    suspend fun uploadPhoto(file: File): Result<CuponeraPhotoUploadResponse> = call {
        val requestBody = file.asRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        api.uploadPhoto(part)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(CuponeraException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(CuponeraException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(CuponeraException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        // El backend devuelve { status, error, timestamp }: el texto está en "error".
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()?.let { body ->
                val json = JSONObject(body)
                json.optString("error").ifBlank { json.optString("message") }
            }?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> serverMessage ?: "Revisá los datos e intentá de nuevo."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            409 -> serverMessage ?: "No se pudo ratificar: ya fue ratificado o el plazo venció."
            413 -> "La foto es demasiado pesada."
            415 -> "La foto debe ser JPEG, PNG o WebP."
            else -> "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}