package com.mrseong.picks.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "login_attempts")
class LoginAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val ipAddress: String,

    @Column(length = 500)
    val userAgent: String? = null,

    @Column(nullable = false)
    val endpoint: String,

    @Column(nullable = false)
    val attemptCount: Int = 1,

    @Column(nullable = false)
    val blocked: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
