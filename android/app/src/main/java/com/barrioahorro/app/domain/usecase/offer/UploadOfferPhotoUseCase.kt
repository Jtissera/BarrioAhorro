package com.barrioahorro.app.domain.usecase.offer

import com.barrioahorro.app.domain.model.OfferError
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import javax.inject.Inject

class UploadOfferPhotoUseCase @Inject constructor(
    private val repository: IOfferRepository,
) {
    suspend operator fun invoke(bytes: ByteArray): Result<String, OfferError> =
        repository.uploadPhoto(bytes)
}

