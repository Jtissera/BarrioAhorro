package com.barrioahorro.app.data.forum

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.DELETE

interface ForumApi {

    @POST("api/foro/publicaciones")
    suspend fun createPost(@Body body: CreateForumPostDto): ForumPostDto

    @GET("api/foro/publicaciones")
    suspend fun getFeed(): List<ForumPostDto>

    @GET("api/foro/mis-publicaciones")
    suspend fun getMyActivity(): MyForumActivityDto

    @GET("api/foro/publicaciones/{id}")
    suspend fun getPostById(@Path("id") id: Long): ForumPostDto

    @GET("api/foro/publicaciones/{id}/comentarios")
    suspend fun getComments(@Path("id") id: Long): List<ForumCommentDto>

    @POST("api/foro/publicaciones/{id}/comentarios")
    suspend fun addComment(
        @Path("id") id: Long,
        @Body body: CreateCommentDto,
    ): ForumCommentDto

    @PATCH("api/foro/publicaciones/{id}")
    suspend fun updatePost(
        @Path("id") id: Long,
        @Body body: UpdateForumPostDto,
    ): ForumPostDto

    @PATCH("api/foro/comentarios/{id}")
    suspend fun updateComment(
        @Path("id") id: Long,
        @Body body: UpdateCommentDto,
    ): ForumCommentDto

    @POST("api/foro/publicaciones/{id}/upvote")
    suspend fun upvotePost(@Path("id") id: Long): UpvoteDto

    @DELETE("api/foro/publicaciones/{id}/upvote")
    suspend fun removePostUpvote(@Path("id") id: Long): UpvoteDto

    @POST("api/foro/comentarios/{id}/upvote")
    suspend fun upvoteComment(@Path("id") id: Long): UpvoteDto

    @DELETE("api/foro/comentarios/{id}/upvote")
    suspend fun removeCommentUpvote(@Path("id") id: Long): UpvoteDto
}
