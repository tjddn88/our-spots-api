package com.ourspots.domain.feedback.repository

import com.ourspots.domain.feedback.entity.Feedback
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface FeedbackRepository : JpaRepository<Feedback, Long> {
    fun countByIpAddressAndCreatedAtAfter(ipAddress: String, since: OffsetDateTime): Long
}
