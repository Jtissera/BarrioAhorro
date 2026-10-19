package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.remote.api.OfferApiService
import com.barrioahorro.app.data.remote.dto.ApiErrorDto
import com.barrioahorro.app.data.remote.dto.CreateOffer2x1RequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferCantidadRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorMayorRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorcentajeRequestDto
import com.barrioahorro.app.data.remote.dto.OfferCreatedDto
import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_UNSUPPORTED_MEDIA_TYPE = 415

class OfferRepositoryImpl @Inject constructor(
    private val offerApiService: OfferApiService,
) : IOfferRepository {

    private val gson = Gson()

    override suspend fun createOffer(offer: NewOffer): Result<Long, OfferError> =
        runCatching {
            when (offer) {
                is NewOffer.Percentage -> offerApiService.createPorcentaje(offer.toDto())
                is NewOffer.LastUnit -> offerApiService.createCantidad(offer.toDto())
                is NewOffer.TwoForOne -> offerApiService.create2x1(offer.toDto())
                is NewOffer.Wholesale -> offerApiService.createPorMayor(offer.toDto())
            }
        }.fold(
            onSuccess = { response -> response.toResult() },
            onFailure = { it.toFailure() },
        )

    override suspend fun updateOffer(id: Long, offer: NewOffer): Result<Long, OfferError> =
        runCatching {
            when (offer) {
                is NewOffer.Percentage -> offerApiService.updatePorcentaje(id, offer.toDto())
                is NewOffer.LastUnit -> offerApiService.updateCantidad(id, offer.toDto())
                is NewOffer.TwoForOne -> offerApiService.update2x1(id, offer.toDto())
                is NewOffer.Wholesale -> offerApiService.updatePorMayor(id, offer.toDto())
            }
        }.fold(
            onSuccess = { response -> response.toResult() },
            onFailure = { it.toFailure() },
        )

    override suspend fun getOffer(id: Long): Result<OfferResponseDto, OfferError> =
        try {
            val response = offerApiService.getOffer(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body)
            } else {
                Result.Failure(response.toError("No pudimos cargar la oferta"))
            }
        } catch (e: IOException) {
            Result.Failure(OfferError.NoConnection)
        } catch (e: Exception) {
            Result.Failure(OfferError.Unknown(e.message))
        }

    override suspend fun deleteOffer(id: Long): Result<Unit, OfferError> =
        try {
            val response = offerApiService.deleteOffer(id)
            if (response.isSuccessful) Result.Success(Unit) else Result.Failure(response.toError("No pudimos eliminar la oferta"))
        } catch (e: IOException) {
            Result.Failure(OfferError.NoConnection)
        } catch (e: Exception) {
            Result.Failure(OfferError.Unknown(e.message))
        }

    override suspend fun uploadPhoto(bytes: ByteArray): Result<String, OfferError> {
        return try {
            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", "oferta.jpg", body)
            val response = offerApiService.uploadPhoto(part)
            val url = response.body()?.url

            if (response.isSuccessful && url != null) {
                Result.Success(url)
            } else {
                Result.Failure(
                    when (response.code()) {
                        HTTP_FORBIDDEN -> response.toError("")
                        HTTP_BAD_REQUEST, HTTP_UNSUPPORTED_MEDIA_TYPE -> OfferError.Rejected("La foto debe ser JPEG, PNG o WebP")
                        else -> OfferError.Unknown("HTTP ${response.code()}")
                    },
                )
            }
        } catch (e: IOException) {
            Result.Failure(OfferError.NoConnection)
        }
    }

    override suspend fun getMyOffers(): Result<List<OfferResponseDto>, OfferError> {
        return try {
            val response = offerApiService.getMyOffers()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(response.toError("Error al obtener ofertas (${response.code()})"))
            }
        } catch (e: IOException) {
            Result.Failure(OfferError.NoConnection)
        } catch (e: Exception) {
            Result.Failure(OfferError.Unknown(e.message))
        }
    }

    // ---- helpers ----

    private fun Response<OfferCreatedDto>.toResult(): Result<Long, OfferError> {
        val body = body()
        if (isSuccessful && body != null) return Result.Success(body.id)
        return Result.Failure(toError("Error del servidor (${code()})"))
    }

    private fun Response<*>.toError(default: String): OfferError {
        val message = runCatching { gson.fromJson(errorBody()?.string(), ApiErrorDto::class.java)?.error }
            .getOrNull()
        return when (code()) {
            HTTP_BAD_REQUEST -> OfferError.Rejected(message ?: "Revisá los datos de la oferta")
            HTTP_FORBIDDEN -> message?.let { OfferError.Rejected(it) } ?: OfferError.NotABusiness
            HTTP_NOT_FOUND -> OfferError.Rejected(message ?: "La oferta ya no existe")
            else -> OfferError.Unknown(message ?: default)
        }
    }

    private fun Throwable.toFailure(): Result<Long, OfferError> =
        Result.Failure(
            when (this) {
                is IOException -> OfferError.NoConnection
                else -> OfferError.Unknown(message)
            },
        )

    private fun LocalDateTime.toIsoWithOffset(): String =
        atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

    private fun NewOffer.Percentage.toDto() = CreateOfferPorcentajeRequestDto(
        nombreProducto = productName.trim(),
        vigenciaTipo = vigenciaTipo,
        vigenteHasta = validUntil?.toIsoWithOffset(),
        precioOriginal = originalPrice,
        porcentajeDescuento = discountPercent,
        fotoUrl = photoUrl,
    )

    private fun NewOffer.LastUnit.toDto() = CreateOfferCantidadRequestDto(
        nombreProducto = productName.trim(),
        vigenciaTipo = vigenciaTipo,
        vigenteHasta = validUntil?.toIsoWithOffset(),
        cantidadRequerida = requiredQuantity,
        precioUnitario = unitPrice,
        porcentajeDescuentoUltimaUnidad = lastUnitDiscountPercent,
        fotoUrl = photoUrl,
    )

    private fun NewOffer.TwoForOne.toDto() = CreateOffer2x1RequestDto(
        nombreProducto = productName.trim(),
        vigenciaTipo = vigenciaTipo,
        vigenteHasta = validUntil?.toIsoWithOffset(),
        unidadesAPagar = unitsToPay,
        unidadesALlevar = unitsToTake,
        precioUnitario = unitPrice,
        fotoUrl = photoUrl,
    )

    private fun NewOffer.Wholesale.toDto() = CreateOfferPorMayorRequestDto(
        nombreProducto = productName.trim(),
        vigenciaTipo = vigenciaTipo,
        vigenteHasta = validUntil?.toIsoWithOffset(),
        cantidadMinima = minQuantity,
        precioUnitarioMayorista = wholesalePrice,
        precioUnitarioRegular = regularPrice,
        fotoUrl = photoUrl,
    )
}