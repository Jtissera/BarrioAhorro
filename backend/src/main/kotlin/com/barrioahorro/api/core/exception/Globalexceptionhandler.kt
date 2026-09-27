package com.barrioahorro.api.core.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<ApiErrorResponse> =
        buildResponse(HttpStatus.CONFLICT, ex.message.orEmpty())

    @ExceptionHandler(PasswordMismatchException::class)
    fun handlePasswordMismatch(ex: PasswordMismatchException): ResponseEntity<ApiErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message.orEmpty())

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiErrorResponse> =
        buildResponse(HttpStatus.UNAUTHORIZED, ex.message.orEmpty())

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(separator = ". ") { it.defaultMessage ?: "Dato inválido" }
        return buildResponse(HttpStatus.BAD_REQUEST, message)
    }

    private fun buildResponse(status: HttpStatus, message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(status.value(), message))
}