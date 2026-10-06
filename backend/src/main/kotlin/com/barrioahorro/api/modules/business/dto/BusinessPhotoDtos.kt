package com.barrioahorro.api.modules.business.dto

data class PhotoResponse(
    val id: Long,
    val url: String,
    val orden: Int,
)

data class ReorderPhotosRequest(
    val photoIds: List<Long>,
)
