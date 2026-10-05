package com.barrioahorro.api.modules.location

import com.barrioahorro.api.modules.location.dto.AddressErrorCode
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.location.dto.ValidateAddressResponse
import org.springframework.stereotype.Service

@Service
class LocationService(
    private val georefClient: GeorefClient,
) {

    fun validarDireccion(request: ValidateAddressRequest): AddressValidationResult {
        val respuesta = georefClient.buscarDireccion(
            direccion = request.direccion.trim(),
            provincia = request.provincia?.takeIf { it.isNotBlank() },
        )

        val primera = respuesta.direcciones.firstOrNull()
            ?: return AddressValidationResult.Invalid(AddressErrorCode.ADDRESS_NOT_FOUND)

        if (primera.altura?.valor == null) {
            return AddressValidationResult.Invalid(AddressErrorCode.MISSING_NUMBER)
        }

        val nomenclatura = primera.nomenclatura
        val lat = primera.ubicacion?.lat
        val lon = primera.ubicacion?.lon
        if (nomenclatura.isNullOrBlank() || lat == null || lon == null) {
            return AddressValidationResult.Invalid(AddressErrorCode.ADDRESS_NOT_FOUND)
        }

        return AddressValidationResult.Valid(
            ValidateAddressResponse(
                direccionNormalizada = nomenclatura,
                latitud = lat,
                longitud = lon,
            )
        )
    }
}

sealed class AddressValidationResult {
    data class Valid(val address: ValidateAddressResponse) : AddressValidationResult()
    data class Invalid(val code: AddressErrorCode) : AddressValidationResult()
}