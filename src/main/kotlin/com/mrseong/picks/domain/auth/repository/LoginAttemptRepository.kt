package com.mrseong.picks.domain.auth.repository

import com.mrseong.picks.domain.auth.entity.LoginAttempt
import org.springframework.data.jpa.repository.JpaRepository

interface LoginAttemptRepository : JpaRepository<LoginAttempt, Long> {
    fun findByIpAddressOrderByCreatedAtDesc(ipAddress: String): List<LoginAttempt>
}
