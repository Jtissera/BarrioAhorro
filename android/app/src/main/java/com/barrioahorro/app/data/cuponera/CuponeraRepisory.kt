package com.barrioahorro.app.data.cuponera

import org.json.JSONObject
import retrofit2.HttpException
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
        // Solo llega si el backend tiene server.error.include-message=always.
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("message") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> "Revisá los datos de los productos."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            409 -> serverMessage ?: "No se pudo ratificar: el aporte ya fue ratificado o el plazo venció."
            else -> "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}