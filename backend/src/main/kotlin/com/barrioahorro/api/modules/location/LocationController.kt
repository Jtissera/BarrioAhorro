package com.barrioahorro.api.modules.location

import com.barrioahorro.api.modules.location.dto.AddressCandidateResponse
import com.barrioahorro.api.modules.location.dto.AddressErrorCode
import com.barrioahorro.api.modules.location.dto.AddressErrorResponse
import com.barrioahorro.api.modules.location.dto.ValidateAddressRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/location")
class LocationController(
    private val locationService: LocationService,
) {

    @PostMapping("/validate")
    fun validate(@Valid @RequestBody request: ValidateAddressRequest): ResponseEntity<*> =
        when (val result = locationService.validarDireccion(request)) {
            is AddressValidationResult.Valid ->
                ResponseEntity.ok(result.address)

            is AddressValidationResult.Invalid -> {
                val status = when (result.code) {
                    AddressErrorCode.GEOREF_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
                    else -> HttpStatus.UNPROCESSABLE_ENTITY
                }
                ResponseEntity.status(status).body(AddressErrorResponse(result.code))
            }
        }

    @PostMapping("/candidates")
    fun candidates(@Valid @RequestBody request: ValidateAddressRequest): List<AddressCandidateResponse> =
        locationService.buscarCandidatos(request)
}