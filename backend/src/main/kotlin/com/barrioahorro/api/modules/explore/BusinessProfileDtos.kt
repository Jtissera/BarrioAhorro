package com.barrioahorro.api.modules.explore

import com.barrioahorro.api.modules.business.dto.BusinessReviewResponse
import com.barrioahorro.api.modules.business.dto.ReviewsSummaryResponse
import com.barrioahorro.api.modules.business.dto.ScheduleSlotResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class BusinessProfileResponse(
    val id: Long,
    val name: String?,
    val category: String?,
    val address: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    /** null si el vecino no compartió su ubicación. */
    val distanceMeters: Int?,
    /** Única foto del comercio (la de portada), ruta relativa. */
    val photoUrl: String?,
    val schedule: List<ScheduleSlotResponse>,
)

/**
 * [mine] es la reseña del vecino que consulta (si existe) y NO se repite en [items],
 * así la app la muestra arriba con la marca "Tu reseña".
 */
data class BusinessReviewsPage(
    val summary: ReviewsSummaryResponse,
    val mine: BusinessReviewResponse?,
    val items: List<BusinessReviewResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class WriteReviewRequest(
    @field:NotNull(message = "Elegí cuántas estrellas le das al comercio")
    @field:Min(value = 1, message = "La calificación mínima es 1 estrella")
    @field:Max(value = 5, message = "La calificación máxima es 5 estrellas")
    val rating: Int? = null,

    @field:NotNull(message = "Escribí tu reseña")
    val comment: String? = null,
)
