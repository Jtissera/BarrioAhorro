package com.barrioahorro.app.data.remote.dto

data class AddressCandidateDto(
    val label: String,
    val locality: String?,
    val latitude: Double,
    val longitude: Double,
    val exact: Boolean,
)