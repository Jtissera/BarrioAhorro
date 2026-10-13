package com.barrioahorro.api.modules.location

import com.barrioahorro.api.modules.location.dto.AddressErrorCode
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import com.barrioahorro.api.modules.location.dto.ValidateAddressResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class LocationService(
    private val nominatimClient: NominatimClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun validarDireccion(request: ValidateAddressRequest): AddressValidationResult {
        val direccion = request.direccion.trim()
        val localidad = request.localidad?.trim()?.takeIf { it.isNotBlank() }
        val provincia = request.provincia?.trim()?.takeIf { it.isNotBlank() }

        val (calle, altura) = separarCalleYAltura(direccion)
            ?: return AddressValidationResult.Invalid(AddressErrorCode.MISSING_NUMBER)

        val resultados = try {
            nominatimClient.buscarDireccion(calle, altura, localidad, provincia)
        } catch (e: NominatimUnavailableException) {
            log.warn("Nominatim no disponible: {}", e.message, e)
            // Se mantiene el código para no romper el contrato con la app
            return AddressValidationResult.Invalid(AddressErrorCode.GEOREF_UNAVAILABLE)
        }

        if (resultados.isEmpty()) {
            return AddressValidationResult.Invalid(AddressErrorCode.ADDRESS_NOT_FOUND)
        }

        val valido = resultados.firstOrNull { coincide(it, altura, localidad) }
        if (valido == null) {

            val sinAltura = resultados.all { it.address?.houseNumber.isNullOrBlank() }
            return AddressValidationResult.Invalid(
                if (sinAltura) AddressErrorCode.MISSING_NUMBER else AddressErrorCode.ADDRESS_NOT_FOUND
            )
        }

        val lat = valido.lat?.toDoubleOrNull()
        val lon = valido.lon?.toDoubleOrNull()
        if (lat == null || lon == null) {
            return AddressValidationResult.Invalid(AddressErrorCode.ADDRESS_NOT_FOUND)
        }

        return AddressValidationResult.Valid(
            ValidateAddressResponse(
                direccionNormalizada = armarNomenclatura(valido),
                latitud = lat,
                longitud = lon,
            )
        )
    }

    private fun coincide(r: NominatimResult, alturaPedida: String, localidadPedida: String?): Boolean {
        val addr = r.address ?: return false
        if (addr.road.isNullOrBlank()) return false

        val alturas = addr.houseNumber?.split(';', ',', '-')?.map { it.trim().lowercase() }.orEmpty()
        if (alturaPedida.lowercase() !in alturas) return false

        if (localidadPedida != null) {
            val pedida = normalizar(localidadPedida)
            val ok = addr.localidades().any { normalizar(it) == pedida }
            if (!ok) return false
        }
        return true
    }

    private fun armarNomenclatura(r: NominatimResult): String {
        val a = r.address
        val partes = listOfNotNull(
            listOfNotNull(a?.road, a?.houseNumber).joinToString(" ").takeIf { it.isNotBlank() },
            a?.localidadPrincipal(),
            a?.state,
        )
        return partes.joinToString(", ").ifBlank { r.displayName.orEmpty() }
    }

    private fun separarCalleYAltura(direccion: String): Pair<String, String>? {
        val m = Regex("""^(.+?)[\s,]+(\d{1,6}[A-Za-z]?)\s*$""").find(direccion) ?: return null
        val calle = m.groupValues[1].trim().trimEnd(',')
        val altura = m.groupValues[2]
        return if (calle.isBlank()) null else calle to altura
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

sealed class AddressValidationResult {
    data class Valid(val address: ValidateAddressResponse) : AddressValidationResult()
    data class Invalid(val code: AddressErrorCode) : AddressValidationResult()
}