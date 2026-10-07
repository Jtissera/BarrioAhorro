package com.barrioahorro.api.modules.location.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeorefResponse(
    val cantidad: Int,
    val direcciones: List<GeorefDireccion>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeorefDireccion(
    val nomenclatura: String?,
    val altura: GeorefAltura?,
    val ubicacion: GeorefUbicacion?,
)

// "altura": { "unidad": null, "valor": 3274 }
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeorefAltura(
    val valor: Int?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeorefUbicacion(
    val lat: Double?,
    val lon: Double?,
)