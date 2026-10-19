package com.barrioahorro.app.domain.repository

import com.barrioahorro.app.domain.model.AddressCandidate
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.model.ValidatedAddress

interface ILocationRepository {

    suspend fun validateAddress(
        address: String,
        province: String?,
        locality: String?,
    ): Result<ValidatedAddress, AddressError>

    suspend fun searchCandidates(
        address: String,
        locality: String?,
    ): Result<List<AddressCandidate>, AddressError>
}