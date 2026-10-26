package com.barrioahorro.app.data.forum

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ForumApi {

    @POST("api/foro/publicaciones")
    suspend fun createPost(@Body body: CreateForumPostDto): ForumPostDto

    @GET("api/foro/publicaciones")
    suspend fun getFeed(): List<ForumPostDto>

    @GET("api/foro/publicaciones/{id}")
    suspend fun getPostById(@Path("id") id: Long): ForumPostDto

    @GET("api/foro/publicaciones/{id}/comentarios")
    suspend fun getComments(@Path("id") id: Long): List<ForumCommentDto>

    @POST("api/foro/publicaciones/{id}/comentarios")
    suspend fun addComment(
        @Path("id") id: Long,
        @Body body: CreateCommentDto,
    ): ForumCommentDto
}
