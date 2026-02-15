package com.ourspots.domain.guestbook.repository

import com.ourspots.domain.guestbook.entity.GuestbookMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface GuestbookRepository : JpaRepository<GuestbookMessage, Long> {

    fun findByOrderByCreatedAtDesc(pageable: Pageable): List<GuestbookMessage>

    fun countByIpAddressAndCreatedAtAfter(ipAddress: String, since: LocalDateTime): Long

    @Query(
        value = "SELECT COUNT(*) FROM guestbook_messages WHERE created_at > :since",
        nativeQuery = true
    )
    fun countAllIncludingDeletedAfter(since: LocalDateTime): Long
}
