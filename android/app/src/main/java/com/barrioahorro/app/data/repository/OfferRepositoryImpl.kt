package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.remote.api.OfferApiService
import com.barrioahorro.app.data.remote.dto.ApiErrorDto
import com.barrioahorro.app.data.remote.dto.CreateOffer2x1RequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferCantidadRequestDto
import com.barrioahorro.app.data.remote.dto.CreateOfferPorcentajeRequestDto
import com.barrioahorro.app.data.remote.dto.OfferCreatedDto
import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_UNSUPPORTED_MEDIA_TYPE = 415
private const val VIGENCIA_FECHA_ESPECIFICA = "FECHA_ESPECIFICA"

class OfferRepositoryImpl @Inject constructor(
    private val offerApiService: OfferApiService,
) : IOfferRepository {

    private val gson = Gson()

    override suspend fun createOffer(offer: NewOffer): Result<Long, OfferError> =
        runCatching {
            when (offer) {
                is NewOffer.Percentage -> offerApiService.createPorcentaje(
                    CreateOfferPorcentajeRequestDto(
                        nombreProducto = offer.productName.trim(),
                        vigenciaTipo = offer.vigenciaTipo,
                        vigenteHasta = offer.validUntil?.toIsoWithOffset(),
                        precioOriginal = offer.originalPrice,
                        porcentajeDescuento = offer.discountPercent,
                        fotoUrl = offer.photoUrl,
                    ),
                )
                is NewOffer.LastUnit -> offerApiService.createCantidad(
                    CreateOfferCantidadRequestDto(
                        nombreProducto = offer.productName.trim(),
                        vigenciaTipo = offer.vigenciaTipo,
                        vigenteHasta = offer.validUntil?.toIsoWithOffset(),
                        cantidadRequerida = offer.requiredQuantity,
                        precioUnitario = offer.unitPrice,
                        porcentajeDescuentoUltimaUnidad = offer.lastUnitDiscountPercent,
                        fotoUrl = offer.photoUrl,
                    ),
                )
                is NewOffer.TwoForOne -> offerApiService.create2x1(
                    CreateOffer2x1RequestDto(
                        nombreProducto = offer.productName.trim(),
                        vigenciaTipo = offer.vigenciaTipo,
                        vigenteHasta = offer.validUntil?.toIsoWithOffset(),
                        unidadesAPagar = offer.unitsToPay,
                        unidadesALlevar = offer.unitsToTake,
                        precioUnitario = offer.unitPrice,
                        fotoUrl = offer.photoUrl,
                    ),
                )
            }
        }.fold(
            onSuccess = { response -> response.toResult() },
            onFailure = { it.toFailure() },
        )

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
                        HTTP_FORBIDDEN -> OfferError.NotABusiness
                        HTTP_BAD_REQUEST, HTTP_UNSUPPORTED_MEDIA_TYPE -> OfferError.Rejected("La foto debe ser JPEG, PNG o WebP")
                        else -> OfferError.Unknown("HTTP ${response.code()}")
                    },
                )
            }
        } catch (e: IOException) {
            Result.Failure(OfferError.NoConnection)
        }
    }

    private fun Response<OfferCreatedDto>.toResult(): Result<Long, OfferError> {
        val body = body()
        if (isSuccessful && body != null) return Result.Success(body.id)

        val message = runCatching { gson.fromJson(errorBody()?.string(), ApiErrorDto::class.java)?.error }
            .getOrNull()

        return Result.Failure(
            when (code()) {
                HTTP_BAD_REQUEST -> OfferError.Rejected(message ?: "Revisá los datos de la oferta")
                HTTP_FORBIDDEN -> OfferError.NotABusiness
                else -> OfferError.Unknown(message ?: "Error del servidor (${code()})")
            },
        )
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
}

