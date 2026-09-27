package com.barrioahorro.api.modules.user

import com.barrioahorro.api.modules.user.enum.TipoUsuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime


@Entity
@Table(name = "usuarios")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    val email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_usuario", nullable = false, columnDefinition = "tipo_usuario_enum")
    val tipoUsuario: TipoUsuario,

    @Column(name = "email_verificado", nullable = false)
    var emailVerificado: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)