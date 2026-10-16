package com.barrioahorro.app.data.sorteo

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Error con mensaje listo para mostrarle al usuario. */
class SorteoException(message: String) : Exception(message)

@Singleton
class SorteoRepository @Inject constructor(
    private val api: SorteoApi,
) {

    suspend fun getCurrent(): Result<SorteoCommitmentDto> = call { api.getCurrent() }

    suspend fun ratify(premio: String): Result<SorteoCommitmentDto> =
        call { api.ratify(RatifySorteoRequestDto(premio.trim())) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(SorteoException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(SorteoException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(SorteoException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            400, 403, 409 -> serverMessage ?: "No se pudo ratificar el premio."
            else -> "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}

