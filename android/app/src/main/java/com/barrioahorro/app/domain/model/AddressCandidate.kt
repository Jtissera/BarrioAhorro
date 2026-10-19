package com.barrioahorro.app.domain.model

data class AddressCandidate(
    val label: String,
    val locality: String?,
    val latitude: Double,
    val longitude: Double,
    val exact: Boolean,
)