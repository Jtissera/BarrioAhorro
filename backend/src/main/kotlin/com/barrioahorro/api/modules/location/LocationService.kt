package com.barrioahorro.api.modules.location

import com.barrioahorro.api.modules.location.dto.AddressErrorCode
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.location.dto.ValidateAddressResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LocationService(
    private val georefClient: GeorefClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun validarDireccion(request: ValidateAddressRequest): AddressValidationResult {
        val respuesta = try {
            georefClient.buscarDireccion(
                direccion = request.direccion.trim(),
                localidad = request.localidad?.trim()?.takeIf { it.isNotBlank() },
                provincia = request.provincia?.trim()?.takeIf { it.isNotBlank() },
            )
        } catch (e: GeorefUnavailableException) {
            log.warn("Georef no disponible: {}", e.message, e)
            return AddressValidationResult.Invalid(AddressErrorCode.GEOREF_UNAVAILABLE)
        }

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