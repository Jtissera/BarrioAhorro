package com.barrioahorro.api.modules.sorteos

import java.time.OffsetDateTime

data class ClientRafflesResponse(
    val unlocked: Boolean,
    val unlockThreshold: Int,
    val lifetimePoints: Int,
    val balance: Int,
    val showUnlockAnimation: Boolean,
    val raffles: List<ClientRaffleResponse>,
)

data class ClientRaffleResponse(
    val id: Long,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessId: Long,
    val businessName: String?,
    val ticketCost: Int,
    val drawAt: OffsetDateTime,
    val totalTickets: Int,
    val participants: Int,
    val myTickets: Int,
    /** "1 en N". */
    val oneIn: Int,
)

data class BuyTicketRequest(val quantity: Int = 1)

data class BuyTicketResponse(
    val balance: Int,
    val raffle: ClientRaffleResponse,
    val quantity: Int,
    val ticketNumbers: List<Long>,
    val receiptCode: String,
)

data class MyTicketsResponse(
    val activeCount: Int,
    val items: List<MyRaffleTicketsResponse>,
)

data class MyRaffleTicketsResponse(
    val raffleId: Long,
    /** ACTIVO | CERRADO | SORTEADO | DESIERTO | ANULADO */
    val raffleStatus: String,
    val prizeName: String,
    val prizeDescription: String?,
    val prizePhotoUrl: String?,
    val businessName: String?,
    val drawAt: OffsetDateTime,
    val myTickets: Int,
    val ticketNumbers: List<Long>,
    val pointsSpent: Int,
    val won: Boolean,
    /** Solo en sorteos activos: "1 en N". */
    val oneIn: Int?,
)