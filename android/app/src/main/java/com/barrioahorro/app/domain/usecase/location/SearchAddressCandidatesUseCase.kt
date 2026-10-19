package com.barrioahorro.app.domain.usecase.location

import com.barrioahorro.app.domain.model.AddressCandidate
import com.barrioahorro.app.domain.model.AddressError
import com.barrioahorro.app.domain.repository.ILocationRepository
import com.barrioahorro.app.domain.repository.Result
import javax.inject.Inject

class SearchAddressCandidatesUseCase @Inject constructor(
    private val locationRepository: ILocationRepository,
) {

    suspend operator fun invoke(
        address: String,
        locality: String? = null,
    ): Result<List<AddressCandidate>, AddressError> {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return Result.Failure(AddressError.Empty)

        return locationRepository.searchCandidates(
            address = trimmed,
            locality = locality?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}