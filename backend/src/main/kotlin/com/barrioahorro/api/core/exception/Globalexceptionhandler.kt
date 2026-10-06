package com.barrioahorro.api.core.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.server.ResponseStatusException
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            HttpStatus.BAD_REQUEST,
            "El pedido es inválido o le faltan campos obligatorios",
        )

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            HttpStatus.valueOf(ex.statusCode.value()),
            ex.reason ?: "Error al procesar el pedido",
        )

    private fun buildResponse(status: HttpStatus, message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(status.value(), message))
}