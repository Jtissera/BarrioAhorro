package com.barrioahorro.api.core.exception

sealed class BusinessRuleException(message: String) : RuntimeException(message)

class EmailAlreadyExistsException(email: String) :
    BusinessRuleException("Ya existe una cuenta registrada con el email $email")

class PasswordMismatchException :
    BusinessRuleException("Las contraseñas no coinciden")

class InvalidCredentialsException :
    BusinessRuleException("Email o contraseña incorrectos")