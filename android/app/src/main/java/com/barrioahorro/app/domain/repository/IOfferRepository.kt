package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError

interface IOfferRepository {
    suspend fun createOffer(offer: NewOffer): Result<Long, OfferError>
}
