package com.barrioahorro.app.domain.usecase.auth

import javax.inject.Inject

enum class PasswordStrength {
    NONE,
    WEAK,
    MEDIUM,
    STRONG,
}

private const val MIN_LENGTH = 8

class EvaluatePasswordStrengthUseCase @Inject constructor() {

    operator fun invoke(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE
        if (password.length < MIN_LENGTH) return PasswordStrength.WEAK

        var score = 1
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            1, 2 -> PasswordStrength.WEAK
            3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
}