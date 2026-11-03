package com.barrioahorro.app.data.raffles

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class RafflesDto(
    val unlocked: Boolean,
    val unlockThreshold: Int,
    val lifetimePoints: Int,
    val balance: Int,
    val showUnlockAnimation: Boolean,
    val raffles: List<RaffleDto>,
)

data class RaffleDto(
    val id: Long,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessId: Long,
    val businessName: String?,
    val ticketCost: Int,
    val drawAt: String,
    val totalTickets: Int,
    val participants: Int,
    val myTickets: Int,
    val oneIn: Int,
)

data class BuyTicketDto(
    val balance: Int,
    val raffle: RaffleDto,
)

interface RaffleApi {

    @GET("api/cliente/sorteos")
    suspend fun getRaffles(): RafflesDto

    @POST("api/cliente/sorteos/{id}/tickets")
    suspend fun buyTicket(@Path("id") id: Long): BuyTicketDto

    @POST("api/cliente/sorteos/desbloqueo-visto")
    suspend fun markUnlockSeen(): Response<Unit>
}