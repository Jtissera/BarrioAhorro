package com.barrioahorro.app.data.forum

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

class ForumException(message: String) : Exception(message)

@Singleton
class ForumRepository @Inject constructor(
    private val forumApi: ForumApi,
) {

    suspend fun createPost(dto: CreateForumPostDto): Result<ForumPostDto> = call {
        forumApi.createPost(dto)
    }

    suspend fun getFeed(): Result<List<ForumPostDto>> = call {
        forumApi.getFeed()
    }

    suspend fun getPostById(id: Long): Result<ForumPostDto> = call {
        forumApi.getPostById(id)
    }

    suspend fun getComments(postId: Long): Result<List<ForumCommentDto>> = call {
        forumApi.getComments(postId)
    }

    suspend fun addComment(postId: Long, dto: CreateCommentDto): Result<ForumCommentDto> = call {
        forumApi.addComment(postId, dto)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ForumException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(ForumException(httpMessage(e)))
        } catch (e: IOException) {
            Result.failure(ForumException("No hay conexión. Revisá tu internet e intentá de nuevo."))
        } catch (e: Exception) {
            Result.failure(ForumException("Ocurrió un error inesperado al procesar la solicitud."))
        }

    private fun httpMessage(e: HttpException): String {
        val serverMessage = runCatching {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("error") }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (e.code()) {
            400 -> serverMessage ?: "Revisá los datos ingresados."
            401 -> "Tu sesión venció. Volvé a iniciar sesión."
            403 -> "No tenés permisos para realizar esta acción."
            404 -> serverMessage ?: "No se encontró el recurso solicitado."
            else -> "Error del servidor (${e.code()}). Intentá nuevamente."
        }
    }
}
