package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError

interface IOfferRepository {
    suspend fun createOffer(offer: NewOffer): Result<Long, OfferError>
    suspend fun uploadPhoto(bytes: ByteArray): Result<String, OfferError>
    suspend fun getMyOffers(): Result<List<OfferResponseDto>, OfferError>
    suspend fun getOffer(id: Long): Result<OfferResponseDto, OfferError>
    suspend fun updateOffer(id: Long, offer: NewOffer): Result<Long, OfferError>
    suspend fun deleteOffer(id: Long): Result<Unit, OfferError>
}