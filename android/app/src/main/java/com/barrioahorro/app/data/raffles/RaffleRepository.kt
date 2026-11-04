package com.barrioahorro.app.data.raffles

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

class RaffleException(message: String) : Exception(message)

@Singleton
class RaffleRepository @Inject constructor(
    private val api: RaffleApi,
) {

    suspend fun getRaffles(): Result<RafflesDto> = call { api.getRaffles() }

    suspend fun buyTicket(raffleId: Long, quantity: Int = 1): Result<BuyTicketDto> =
        call { api.buyTicket(raffleId, BuyTicketRequestDto(quantity)) }

    suspend fun getMyTickets(): Result<MyTicketsDto> = call { api.getMyTickets() }

    suspend fun markUnlockSeen(): Result<Unit> = call {
        val response = api.markUnlockSeen()
        if (!response.isSuccessful) throw HttpException(response)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(RaffleException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(RaffleException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(RaffleException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> serverMessage ?: "La cantidad de tickets no es válida."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            404 -> serverMessage ?: "No encontramos ese sorteo."
            409 -> serverMessage ?: "No se pudo completar la acción. Intentá de nuevo."
            else -> serverMessage ?: "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}