package com.barrioahorro.app.data.forum

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ForumApi {

    @POST("api/foro/publicaciones")
    suspend fun createPost(@Body body: CreateForumPostDto): ForumPostDto

    @GET("api/foro/publicaciones")
    suspend fun getFeed(): List<ForumPostDto>
}
