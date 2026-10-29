package com.barrioahorro.api.modules.forum

import com.barrioahorro.api.modules.forum.dto.CreateCommentRequest
import com.barrioahorro.api.modules.forum.dto.CreateForumPostRequest
import com.barrioahorro.api.modules.forum.dto.ForumCommentResponse
import com.barrioahorro.api.modules.forum.dto.ForumPostResponse
import com.barrioahorro.api.modules.forum.dto.MyForumActivityResponse
import com.barrioahorro.api.modules.forum.dto.UpdateCommentRequest
import com.barrioahorro.api.modules.forum.dto.UpdateForumPostRequest
import com.barrioahorro.api.modules.forum.dto.UpvoteResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.DeleteMapping

@RestController
@RequestMapping("/api/foro")
class ForumPostController(
    private val forumPostService: ForumPostService,
    private val forumUpvoteService: ForumUpvoteService,
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
    fun getFeed(authentication: Authentication): ResponseEntity<List<ForumPostResponse>> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.getFeed(userId))
    }

    @GetMapping("/mis-publicaciones")
    fun getMyActivity(authentication: Authentication): ResponseEntity<MyForumActivityResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.getMyActivity(userId))
    }

    @GetMapping("/publicaciones/{id}")
    fun getPostById(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ForumPostResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.getPostById(id, userId))
    }

    @PatchMapping("/publicaciones/{id}")
    fun updatePost(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateForumPostRequest,
    ): ResponseEntity<ForumPostResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.updatePost(userId, id, request))
    }

    @GetMapping("/publicaciones/{id}/comentarios")
    fun getComments(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<List<ForumCommentResponse>> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.getComments(id, userId))
    }

    @PostMapping("/publicaciones/{id}/comentarios")
    fun addComment(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ResponseEntity<ForumCommentResponse> {
        val userId = authentication.principal as Long
        val response = forumPostService.addComment(userId, id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PatchMapping("/comentarios/{id}")
    fun updateComment(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCommentRequest,
    ): ResponseEntity<ForumCommentResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(forumPostService.updateComment(userId, id, request))
    }

    @PostMapping("/publicaciones/{id}/upvote")
    fun upvotePost(authentication: Authentication, @PathVariable id: Long): ResponseEntity<UpvoteResponse> =
        ResponseEntity.ok(forumUpvoteService.upvotePost(authentication.principal as Long, id))

    @DeleteMapping("/publicaciones/{id}/upvote")
    fun removePostUpvote(authentication: Authentication, @PathVariable id: Long): ResponseEntity<UpvoteResponse> =
        ResponseEntity.ok(forumUpvoteService.removePostUpvote(authentication.principal as Long, id))

    @PostMapping("/comentarios/{id}/upvote")
    fun upvoteComment(authentication: Authentication, @PathVariable id: Long): ResponseEntity<UpvoteResponse> =
        ResponseEntity.ok(forumUpvoteService.upvoteComment(authentication.principal as Long, id))

    @DeleteMapping("/comentarios/{id}/upvote")
    fun removeCommentUpvote(authentication: Authentication, @PathVariable id: Long): ResponseEntity<UpvoteResponse> =
        ResponseEntity.ok(forumUpvoteService.removeCommentUpvote(authentication.principal as Long, id))

}
