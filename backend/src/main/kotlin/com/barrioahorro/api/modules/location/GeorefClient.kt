package com.barrioahorro.api.modules.location

import com.barrioahorro.api.modules.location.dto.GeorefResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.Optional

@Component
class GeorefClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${georef.base-url:https://apis.datos.gob.ar/georef/api}") baseUrl: String,
) {

    private val restClient: RestClient = restClientBuilder
        .baseUrl(baseUrl)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(5))
            }
        )
        .build()

    fun buscarDireccion(direccion: String, provincia: String?): GeorefResponse {
        try {
            return restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/direcciones")
                        .queryParam("direccion", direccion)
                        .queryParamIfPresent("provincia", Optional.ofNullable(provincia))
                        .queryParam("max", 1)
                        .build()
                }
                .retrieve()
                .body(GeorefResponse::class.java)
                ?: throw GeorefUnavailableException("Georef devolvió una respuesta vacía")
        } catch (e: RestClientException) {
            throw GeorefUnavailableException("No se pudo consultar Georef", e)
        }
    }
}

class GeorefUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)