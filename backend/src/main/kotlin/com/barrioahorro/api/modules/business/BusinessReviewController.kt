package com.barrioahorro.api.modules.business

import com.barrioahorro.api.modules.business.dto.ReviewsPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/business/me/reviews")
class BusinessReviewController(
    private val reviewService: BusinessReviewService,
) {

    @GetMapping
    fun getMyReviews(
        authentication: Authentication,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "10") size: Int,
    ): ResponseEntity<ReviewsPageResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(reviewService.getMyReviews(userId, page, size))
    }
}