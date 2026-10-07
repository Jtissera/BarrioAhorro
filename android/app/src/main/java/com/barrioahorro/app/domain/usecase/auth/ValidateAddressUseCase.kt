package com.barrioahorro.app.domain.usecase.location

import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.model.ValidatedAddress
import com.barrioahorro.app.domain.repository.ILocationRepository
import com.barrioahorro.app.domain.repository.Result
import javax.inject.Inject

class ValidateAddressUseCase @Inject constructor(
    private val locationRepository: ILocationRepository,
) {

    suspend operator fun invoke(
        address: String,
        province: String? = null,
    ): Result<ValidatedAddress, AddressError> {
        val trimmed = address.trim()

        if (trimmed.isEmpty()) {
            return Result.Failure(AddressError.Empty)
        }

        return locationRepository.validateAddress(trimmed, province)
    }
}

