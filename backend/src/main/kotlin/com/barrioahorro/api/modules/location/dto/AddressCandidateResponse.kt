package com.barrioahorro.api.modules.location.dto

data class AddressCandidateResponse(
    val label: String,
    val locality: String?,
    val latitude: Double,
    val longitude: Double,
    /** false cuando solo se ubicó la calle, sin confirmar la altura. */
    val exact: Boolean,
)