package com.barrioahorro.app.data.profile

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

class ClientProfileException(message: String) : Exception(message)

@Singleton
class ClientProfileRepository @Inject constructor(
    private val api: ClientProfileApi,
) {

    suspend fun getProfile(): Result<ClientProfileDto> = call { api.getProfile() }

    suspend fun getMyReviews(page: Int, size: Int): Result<MyReviewsPageDto> =
        call { api.getMyReviews(page, size) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(ClientProfileException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(ClientProfileException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(ClientProfileException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            else -> serverMessage ?: "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}