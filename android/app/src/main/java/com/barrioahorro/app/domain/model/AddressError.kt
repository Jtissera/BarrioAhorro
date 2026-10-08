package com.barrioahorro.app.domain.model

sealed class AddressError {
    data object Empty : AddressError()
    data object NotFound : AddressError()
    data object MissingNumber : AddressError()
    data object ServiceUnavailable : AddressError()
    data object NoConnection : AddressError()
    data class Unknown(val message: String?) : AddressError()
}