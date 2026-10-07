package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.model.ValidatedAddress

interface ILocationRepository {

    suspend fun validateAddress(
        address: String,
        province: String? = null,
    ): Result<ValidatedAddress, AddressError>
}