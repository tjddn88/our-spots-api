package com.mrseong.picks.domain.auth.service

import com.mrseong.picks.common.exception.TooManyRequestsException
import com.mrseong.picks.common.exception.UnauthorizedException
import com.mrseong.picks.domain.auth.entity.LoginAttempt
import com.mrseong.picks.domain.auth.repository.LoginAttemptRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class AttemptInfo(
    val count: Int,
    val blockedUntil: LocalDateTime?
)

@Service
class AuthService(
    private val loginAttemptRepository: LoginAttemptRepository,
    @Value("\${app.admin-password:}") private val adminPassword: String
) {
    // 메모리에서 실패 횟수 관리 (IP -> AttemptInfo)
    private val attemptCache = ConcurrentHashMap<String, AttemptInfo>()

    companion object {
        const val MAX_ATTEMPTS = 5
        const val BLOCK_DURATION_HOURS = 24L
    }

    fun validatePassword(
        password: String?,
        ipAddress: String,
        userAgent: String?,
        endpoint: String
    ) {
        // 차단 여부 확인
        checkIfBlocked(ipAddress)

        // 비밀번호 확인
        if (password == null || password != adminPassword) {
            handleFailedAttempt(ipAddress, userAgent, endpoint)
            throw UnauthorizedException("권한이 없습니다. 관리자 비밀번호를 확인해주세요")
        }

        // 성공 시 실패 횟수 초기화
        attemptCache.remove(ipAddress)
    }

    private fun checkIfBlocked(ipAddress: String) {
        val info = attemptCache[ipAddress] ?: return

        info.blockedUntil?.let { blockedUntil ->
            if (LocalDateTime.now().isBefore(blockedUntil)) {
                throw TooManyRequestsException(
                    "너무 많은 시도로 인해 차단되었습니다. ${blockedUntil.toLocalDate()} ${blockedUntil.toLocalTime().withNano(0)} 이후에 다시 시도해주세요."
                )
            } else {
                // 차단 시간 지남 - 초기화
                attemptCache.remove(ipAddress)
            }
        }
    }

    private fun handleFailedAttempt(ipAddress: String, userAgent: String?, endpoint: String) {
        val currentInfo = attemptCache[ipAddress]
        val newCount = (currentInfo?.count ?: 0) + 1

        val blockedUntil = if (newCount >= MAX_ATTEMPTS) {
            LocalDateTime.now().plusHours(BLOCK_DURATION_HOURS)
        } else {
            null
        }

        // 메모리에 저장
        attemptCache[ipAddress] = AttemptInfo(newCount, blockedUntil)

        // DB에 기록 (공격자 정보 저장)
        val attempt = LoginAttempt(
            ipAddress = ipAddress,
            userAgent = userAgent?.take(500), // 최대 500자
            endpoint = endpoint,
            attemptCount = newCount,
            blocked = blockedUntil != null
        )
        loginAttemptRepository.save(attempt)
    }

    // 관리자용: 차단 해제
    fun unblockIp(ipAddress: String) {
        attemptCache.remove(ipAddress)
    }

    // 관리자용: 모든 시도 기록 조회
    fun getAttemptsByIp(ipAddress: String): List<LoginAttempt> {
        return loginAttemptRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress)
    }
}
