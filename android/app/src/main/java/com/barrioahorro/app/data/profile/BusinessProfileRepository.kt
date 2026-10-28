package com.barrioahorro.app.data.profile

import com.barrioahorro.app.data.explore.ExploreOfferDto
import com.barrioahorro.app.data.remote.dto.BusinessReviewDto
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Error con mensaje listo para mostrarle al vecino. */
class BusinessProfileException(message: String) : Exception(message)

@Singleton
class BusinessProfileRepository @Inject constructor(
    private val api: BusinessProfileApi,
) {

    suspend fun getProfile(businessId: Long, latitude: Double?, longitude: Double?): Result<BusinessProfileDto> =
        call { api.getProfile(businessId, latitude, longitude) }

    suspend fun getOffers(businessId: Long, latitude: Double?, longitude: Double?): Result<List<ExploreOfferDto>> =
        call { api.getOffers(businessId, latitude, longitude) }

    suspend fun getReviews(businessId: Long, page: Int, size: Int): Result<BusinessReviewsPageDto> =
        call { api.getReviews(businessId, page, size) }

    suspend fun saveReview(businessId: Long, rating: Int, comment: String): Result<BusinessReviewDto> =
        call { api.saveReview(businessId, WriteReviewRequestDto(rating, comment)) }

    suspend fun deleteReview(businessId: Long): Result<Unit> = call {
        val response = api.deleteReview(businessId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(BusinessProfileException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(BusinessProfileException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(BusinessProfileException("Ocurrió un error inesperado. Intentá de nuevo."))
        }

    private fun httpMessage(e: HttpException): String {
        // El backend devuelve { status, error, timestamp }: el texto está en "error".
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> serverMessage ?: "Revisá los datos e intentá de nuevo."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> serverMessage ?: "No tenés permiso para hacer esta acción."
            404 -> serverMessage ?: "No encontramos lo que buscabas."
            else -> "Error del servidor (${e.code()}). Intentá de nuevo."
        }
    }
}
