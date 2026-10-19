package com.barrioahorro.app.data.explore

import com.barrioahorro.app.data.remote.api.CategoryApiService
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

class ExploreException(message: String) : Exception(message)

data class ExploreQuery(
    val latitude: Double?,
    val longitude: Double?,
    val radiusKm: Double?,
    val categoryIds: List<Int>,
    val types: List<String>,
    val text: String?,
    val order: String,
)

@Singleton
class ExploreRepository @Inject constructor(
    private val api: ExploreApi,
    private val categoryApi: CategoryApiService,
) {

    suspend fun getOffers(query: ExploreQuery, page: Int, size: Int): Result<ExploreOffersPageDto> = call {
        api.getOffers(
            latitude = query.latitude,
            longitude = query.longitude,
            radiusKm = query.radiusKm,
            categoryIds = query.categoryIds.takeIf { it.isNotEmpty() },
            types = query.types.takeIf { it.isNotEmpty() },
            text = query.text,
            order = query.order,
            page = page,
            size = size,
        )
    }

    suspend fun getSummary(): Result<ClientSummaryDto> = call { api.getSummary() }

    suspend fun getCategories(): Result<List<ExploreCategory>> = call {
        val response = categoryApi.getCategories()
        if (!response.isSuccessful) throw ExploreException("No pudimos cargar los rubros.")
        response.body().orEmpty().map { ExploreCategory(it.id, it.name) }
    }

    suspend fun setFavorite(offerId: Long, favorite: Boolean): Result<Unit> = call {
        val response = if (favorite) api.addFavorite(offerId) else api.removeFavorite(offerId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExploreException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(ExploreException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(ExploreException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(ExploreException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        // El backend devuelve { status, error, timestamp }: el texto está en "error".
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> serverMessage ?: "Revisá los filtros e intentá de nuevo."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            404 -> serverMessage ?: "No encontramos lo que buscabas."
            else -> "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}