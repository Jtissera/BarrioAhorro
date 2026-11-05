package com.barrioahorro.app.data.raffles

import retrofit2.Response
import retrofit2.http.Body
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

data class BuyTicketRequestDto(val quantity: Int)

data class BuyTicketDto(
    val balance: Int,
    val raffle: RaffleDto,
    val quantity: Int,
    val ticketNumbers: List<Long>,
    val receiptCode: String,
)

data class MyTicketsDto(
    val activeCount: Int,
    val items: List<MyRaffleTicketsDto>,
)

data class MyRaffleTicketsDto(
    val raffleId: Long,
    val raffleStatus: String,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessName: String?,
    val drawAt: String,
    val myTickets: Int,
    val ticketNumbers: List<Long>,
    val pointsSpent: Int,
    val won: Boolean,
    val oneIn: Int?,
    val winningTicketId: Long?,
    val prizeStatus: String?
)

data class WinnerNoticeDto(
    val ticketId: Long,
    val raffleTitle: String,
    val prizeName: String,
    val prizePhotoUrl: String?,
    val businessName: String?,
)

data class PrizeDetailDto(
    val ticketId: Long,
    val ticketNumber: Long,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessName: String?,
    val status: String,
    val expiresAt: String,
    val redeemedAt: String?,
    val qrPayload: String?,
)

data class ScanCodeRequestDto(val code: String)

data class PrizeScanResultDto(
    val valid: Boolean,
    val reason: String?,
    val message: String,
    val prizeName: String?,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val ticketNumber: Long?,
    val winnerName: String?,
    val expiresAt: String?,
)

interface RaffleApi {

    @GET("api/cliente/sorteos")
    suspend fun getRaffles(): RafflesDto

    @POST("api/cliente/sorteos/{id}/tickets")
    suspend fun buyTicket(
        @Path("id") id: Long,
        @Body body: BuyTicketRequestDto,
    ): BuyTicketDto

    @GET("api/cliente/sorteos/mis-tickets")
    suspend fun getMyTickets(): MyTicketsDto

    @POST("api/cliente/sorteos/desbloqueo-visto")
    suspend fun markUnlockSeen(): Response<Unit>

    @GET("api/cliente/sorteos/premios/pendientes")
    suspend fun getPendingWinnerNotices(): List<WinnerNoticeDto>

    @POST("api/cliente/sorteos/premios/{ticketId}/visto")
    suspend fun markWinnerNoticeSeen(@Path("ticketId") ticketId: Long): Response<Unit>

    @GET("api/cliente/sorteos/premios/{ticketId}")
    suspend fun getPrize(@Path("ticketId") ticketId: Long): PrizeDetailDto

    @POST("api/comercio/sorteos/premios/verificar")
    suspend fun verifyPrize(@Body body: ScanCodeRequestDto): PrizeScanResultDto

    @POST("api/comercio/sorteos/premios/canjear")
    suspend fun redeemPrize(@Body body: ScanCodeRequestDto): PrizeScanResultDto
}