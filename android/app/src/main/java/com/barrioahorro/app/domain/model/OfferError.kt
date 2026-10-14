package com.barrioahorro.app.domain.model

sealed class OfferError {
    data object EmptyProductName : OfferError()
    data object InvalidPrice : OfferError()
    data object InvalidPercent : OfferError()
    data object InvalidQuantity : OfferError()
    data object PastDate : OfferError()
    data class Rejected(val message: String) : OfferError()
    data object NotABusiness : OfferError()
    data object NoConnection : OfferError()
    data class Unknown(val message: String?) : OfferError()
}

