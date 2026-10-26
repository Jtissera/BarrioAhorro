package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.forum.dto.CreateForumPostRequest
import com.barrioahorro.api.modules.forum.dto.ForumPostResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/foro")
class ForumPostController(
    private val forumPostService: ForumPostService,
) {

    @PostMapping("/publicaciones")
    fun createPost(
        authentication: Authentication,
        @Valid @RequestBody request: CreateForumPostRequest,
    ): ResponseEntity<ForumPostResponse> {
        val userId = authentication.principal as Long
        val response = forumPostService.createPost(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/publicaciones")
    fun getFeed(): ResponseEntity<List<ForumPostResponse>> {
        val feed = forumPostService.getFeed()
        return ResponseEntity.ok(feed)
    }
}
