package com.barrioahorro.api.modules.location

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

class NominatimUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NominatimResult(
    val lat: String? = null,
    val lon: String? = null,
    @JsonProperty("display_name") val displayName: String? = null,
    val address: NominatimAddress? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NominatimAddress(
    @JsonProperty("house_number") val houseNumber: String? = null,
    val road: String? = null,
    val suburb: String? = null,
    @JsonProperty("city_district") val cityDistrict: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
) {
    /** Todos los nombres de lugar que pueden representar la "localidad" del usuario. */
    fun localidades(): List<String> =
        listOfNotNull(suburb, cityDistrict, city, town, village, municipality, county)

    fun localidadPrincipal(): String? =
        city ?: town ?: village ?: municipality ?: suburb ?: cityDistrict
}

@Component
class NominatimClient(
    @Value("\${nominatim.base-url:https://nominatim.openstreetmap.org}") baseUrl: String,
    @Value("\${nominatim.user-agent}") userAgent: String,
    @Value("\${nominatim.timeout-seconds:5}") timeoutSeconds: Long,
) {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("User-Agent", userAgent) // obligatorio
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                setReadTimeout(Duration.ofSeconds(timeoutSeconds))
            }
        )
        .build()


    fun buscarDireccion(
        calle: String,
        altura: String,
        localidad: String?,
        provincia: String?,
    ): List<NominatimResult> =
        buscar(calle, altura, localidad, provincia)

    fun buscarCalle(
        calle: String,
        localidad: String?,
        provincia: String?,
    ): List<NominatimResult> =
        buscar(calle, null, localidad, provincia)

    private fun buscar(
        calle: String,
        altura: String?,
        localidad: String?,
        provincia: String?,
    ): List<NominatimResult> {
        return try {
            restClient.get()
                .uri { uri ->
                    uri.path("/search")
                        .queryParam(
                            "street",
                            listOfNotNull(altura, calle).joinToString(" "),
                        )
                        .apply {
                            localidad?.let { queryParam("city", it) }
                            provincia?.let { queryParam("state", it) }
                        }
                        .queryParam("country", "Argentina")
                        .queryParam("countrycodes", "ar")
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .queryParam("limit", 5)
                        .queryParam("accept-language", "es")
                        .build()
                }
                .retrieve()
                .body(Array<NominatimResult>::class.java)
                ?.toList()
                ?: emptyList()
        } catch (e: RestClientException) {
            throw NominatimUnavailableException(
                "Error consultando Nominatim: ${e.message}",
                e,
            )
        }
    }
}

