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

    fun validarDireccion(
        request: ValidateAddressRequest,
    ): AddressValidationResult {
        val direccion = request.direccion.trim()
        val localidad = request.localidad
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val provincia = request.provincia
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val (calle, altura) = separarCalleYAltura(direccion)
            ?: return AddressValidationResult.Invalid(
                AddressErrorCode.MISSING_NUMBER
            )

        return try {
            val resultadosExactos = nominatimClient.buscarDireccion(
                calle = calle,
                altura = altura,
                localidad = localidad,
                provincia = provincia,
            )

            val exacto = resultadosExactos.firstOrNull {
                coincide(it, altura, localidad)
            }

            if (exacto != null) {
                val lat = exacto.lat?.toDoubleOrNull()
                val lon = exacto.lon?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    return AddressValidationResult.Valid(
                        ValidateAddressResponse(
                            direccionNormalizada = armarNomenclatura(exacto),
                            latitud = lat,
                            longitud = lon,
                            alturaEncontrada = true,
                            mensaje = null,
                        )
                    )
                }
            }

            // 2. Si no se pudo confirmar el número, buscamos la calle sola.
            val resultadosCalle = nominatimClient.buscarCalle(
                calle = calle,
                localidad = localidad,
                provincia = provincia,
            )

            val resultadoCalle = resultadosCalle.firstOrNull {
                coincideCalleYLocalidad(it, calle, localidad)
            }

            if (resultadoCalle != null) {
                val lat = resultadoCalle.lat?.toDoubleOrNull()
                val lon = resultadoCalle.lon?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    log.info(
                        "Se encontró la calle, pero no se confirmó la altura. " +
                                "Calle={}, altura={}, localidad={}",
                        calle,
                        altura,
                        localidad,
                    )

                    return AddressValidationResult.Valid(
                        ValidateAddressResponse(
                            direccionNormalizada =
                                armarNomenclatura(resultadoCalle),
                            latitud = lat,
                            longitud = lon,
                            alturaEncontrada = false,
                            mensaje = "No se encontró la altura $altura. " +
                                    "Se muestra una ubicación aproximada de la calle.",
                        )
                    )
                }
            }

            log.info(
                "No se encontró la dirección ni una ubicación alternativa. " +
                        "Calle={}, altura={}, localidad={}, resultadosExactos={}",
                calle,
                altura,
                localidad,
                resultadosExactos.size,
            )

            AddressValidationResult.Invalid(
                AddressErrorCode.ADDRESS_NOT_FOUND
            )
        } catch (e: NominatimUnavailableException) {
            log.warn("Nominatim no disponible: {}", e.message, e)

            AddressValidationResult.Invalid(
                AddressErrorCode.GEOREF_UNAVAILABLE
            )
        }
    }

    private fun coincide(
        r: NominatimResult,
        alturaPedida: String,
        localidadPedida: String?,
    ): Boolean {
        val addr = r.address ?: return false
        if (addr.road.isNullOrBlank()) return false

        val alturas = addr.houseNumber
            ?.split(';', ',', '-')
            ?.map { it.trim().lowercase() }
            .orEmpty()

        if (alturaPedida.lowercase() !in alturas) return false

        return coincideLocalidad(addr, localidadPedida)
    }

    private fun coincideCalleYLocalidad(
        r: NominatimResult,
        callePedida: String,
        localidadPedida: String?,
    ): Boolean {
        val addr = r.address ?: return false
        val calleEncontrada = addr.road ?: return false

        // No aceptamos una calle con otro nombre.
        if (normalizar(calleEncontrada) != normalizar(callePedida)) {
            return false
        }

        return coincideLocalidad(addr, localidadPedida)
    }

    private fun coincideLocalidad(
        addr: NominatimAddress,
        localidadPedida: String?,
    ): Boolean {
        if (localidadPedida == null) return true

        val pedida = normalizar(localidadPedida)

        return addr.localidades().any {
            normalizar(it) == pedida
        }
    }

    private fun armarNomenclatura(
        r: NominatimResult,
    ): String {
        val a = r.address

        // No agregamos una altura cuando la ubicación es aproximada.
        val partes = listOfNotNull(
            a?.road,
            a?.localidadPrincipal(),
            a?.state,
        )

        return partes.joinToString(", ").ifBlank {
            r.displayName.orEmpty()
        }
    }

    private fun separarCalleYAltura(
        direccion: String,
    ): Pair<String, String>? {
        val m = Regex(
            """^(.+?)[\s,]+(\d{1,6}[A-Za-z]?)\s*$"""
        ).find(direccion) ?: return null

        val calle = m.groupValues[1].trim().trimEnd(',')
        val altura = m.groupValues[2]

        return if (calle.isBlank()) null else calle to altura
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(
            s.trim().lowercase(),
            Normalizer.Form.NFD,
        ).replace(
            Regex("\\p{InCombiningDiacriticalMarks}+"),
            "",
        )
}

sealed class AddressValidationResult {
    data class Valid(
        val address: ValidateAddressResponse,
    ) : AddressValidationResult()

    data class Invalid(
        val code: AddressErrorCode,
    ) : AddressValidationResult()
}
