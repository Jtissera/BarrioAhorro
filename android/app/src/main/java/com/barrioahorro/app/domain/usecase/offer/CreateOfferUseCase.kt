package com.barrioahorro.app.domain.usecase.offer

import com.barrioahorro.app.domain.model.NewOffer
import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

class CreateOfferUseCase @Inject constructor(
    private val offerRepository: IOfferRepository,
) {
    suspend operator fun invoke(offer: NewOffer): Result<Long, OfferError> {
        validate(offer)?.let { return Result.Failure(it) }
        return offerRepository.createOffer(offer)
    }

    private fun validate(offer: NewOffer): OfferError? {
        if (offer.productName.isBlank()) return OfferError.EmptyProductName
        if (!offer.validUntil.isAfter(LocalDateTime.now())) return OfferError.PastDate

        return when (offer) {
            is NewOffer.Percentage -> when {
                offer.originalPrice <= BigDecimal.ZERO -> OfferError.InvalidPrice
                offer.discountPercent !in 1..99 -> OfferError.InvalidPercent
                else -> null
            }
            is NewOffer.LastUnit -> when {
                offer.unitPrice <= BigDecimal.ZERO -> OfferError.InvalidPrice
                offer.requiredQuantity < 2 -> OfferError.InvalidQuantity
                offer.lastUnitDiscountPercent !in 1..99 -> OfferError.InvalidPercent
                else -> null
            }
        }
    }
}

