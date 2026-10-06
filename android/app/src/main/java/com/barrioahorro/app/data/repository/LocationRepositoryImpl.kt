package com.barrioahorro.app.data.repository

import com.barrioahorro.app.data.remote.api.LocationApiService
import com.barrioahorro.app.data.remote.dto.AddressErrorDto
import com.barrioahorro.app.data.remote.dto.ValidateAddressRequestDto
import com.barrioahorro.app.data.remote.dto.ValidateAddressResponseDto
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.model.ValidatedAddress
import com.barrioahorro.app.domain.repository.ILocationRepository
import com.barrioahorro.app.domain.repository.Result
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val CODE_MISSING_NUMBER = "MISSING_NUMBER"

class LocationRepositoryImpl @Inject constructor(
    private val locationApiService: LocationApiService,
) : ILocationRepository {

    private val gson = Gson()

    override suspend fun validateAddress(
        address: String,
        province: String?,
    ): Result<ValidatedAddress, AddressError> {
        val request = ValidateAddressRequestDto(direccion = address, provincia = province)
        return runCatching { locationApiService.validateAddress(request) }
            .fold(
                onSuccess = { response -> response.toResult() },
                onFailure = { it.toFailure() },
            )
    }

    private fun Response<ValidateAddressResponseDto>.toResult(): Result<ValidatedAddress, AddressError> {
        val body = body()
        if (isSuccessful && body != null) {
            return Result.Success(
                ValidatedAddress(
                    formattedAddress = body.direccionNormalizada,
                    latitude = body.latitud,
                    longitude = body.longitud,
                ),
            )
        }
        return Result.Failure(mapHttpError(code(), errorBody()?.string()))
    }

    private fun mapHttpError(httpCode: Int, rawErrorBody: String?): AddressError = when (httpCode) {
        HTTP_UNPROCESSABLE -> when (parseErrorCode(rawErrorBody)) {
            CODE_MISSING_NUMBER -> AddressError.MissingNumber
            else -> AddressError.NotFound
        }
        HTTP_SERVICE_UNAVAILABLE -> AddressError.ServiceUnavailable
        else -> AddressError.Unknown(message = "Error del servidor ($httpCode)")
    }

    private fun parseErrorCode(rawErrorBody: String?): String? =
        runCatching { gson.fromJson(rawErrorBody, AddressErrorDto::class.java)?.error }
            .getOrNull()

    private fun Throwable.toFailure(): Result<ValidatedAddress, AddressError> =
        Result.Failure(
            when (this) {
                is IOException -> AddressError.NoConnection
                else -> AddressError.Unknown(message = message)
            },
        )
}

