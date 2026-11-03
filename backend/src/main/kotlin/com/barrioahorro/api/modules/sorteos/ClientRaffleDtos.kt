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

data class BuyTicketResponse(
    val balance: Int,
    val raffle: ClientRaffleResponse,
)