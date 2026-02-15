package com.ourspots.domain.guestbook.entity

import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "guestbook_messages")
@SQLDelete(sql = "UPDATE guestbook_messages SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class GuestbookMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(length = 20)
    var nickname: String? = null,

    @Column(nullable = false, length = 200)
    var content: String,

    @Column(nullable = false, length = 45)
    var ipAddress: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var deletedAt: LocalDateTime? = null
)
