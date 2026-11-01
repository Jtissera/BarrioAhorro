package com.barrioahorro.api.modules.forum

data class ForumFeedFilters(
    val comercioId: Long? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double = 2.0,
    val dias: Int? = null,
)