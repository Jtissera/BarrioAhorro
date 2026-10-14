package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.remote.api.OfferApiService
import com.barrioahorro.app.data.remote.dto.ApiErrorDto
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

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
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
                        vigenciaTipo = VIGENCIA_FECHA_ESPECIFICA,
                        vigenteHasta = offer.validUntil.toIsoWithOffset(),
                        precioOriginal = offer.originalPrice,
                        porcentajeDescuento = offer.discountPercent,
                    ),
                )
                is NewOffer.LastUnit -> offerApiService.createCantidad(
                    CreateOfferCantidadRequestDto(
                        nombreProducto = offer.productName.trim(),
                        vigenciaTipo = VIGENCIA_FECHA_ESPECIFICA,
                        vigenteHasta = offer.validUntil.toIsoWithOffset(),
                        cantidadRequerida = offer.requiredQuantity,
                        precioUnitario = offer.unitPrice,
                        porcentajeDescuentoUltimaUnidad = offer.lastUnitDiscountPercent,
                    ),
                )
            }
        }.fold(
            onSuccess = { response -> response.toResult() },
            onFailure = { it.toFailure() },
        )

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

