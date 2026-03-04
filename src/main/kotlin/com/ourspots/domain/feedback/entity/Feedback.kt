package com.ourspots.domain.feedback.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "feedbacks")
class Feedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 500)
    var content: String,

    @Column(length = 45)
    var ipAddress: String? = null,

    @Column(updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    @Column(length = 50)
    var source: String? = null
) {
    @PrePersist
    fun prePersist() {
        if (source == null) source = "our-spots"
    }
}
