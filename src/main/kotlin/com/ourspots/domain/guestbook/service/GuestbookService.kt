package com.ourspots.domain.guestbook.service

import com.ourspots.api.dto.GuestbookCreateRequest
import com.ourspots.api.dto.GuestbookMessageResponse
import com.ourspots.common.exception.NotFoundException
import com.ourspots.common.exception.TooManyRequestsException
import com.ourspots.common.exception.UnauthorizedException
import com.ourspots.domain.guestbook.entity.GuestbookMessage
import com.ourspots.domain.guestbook.repository.GuestbookRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class GuestbookService(
    private val guestbookRepository: GuestbookRepository
) {
    private val lastWriteTimeByIp = ConcurrentHashMap<String, LocalDateTime>()

    companion object {
        private const val MAX_DISPLAY = 20
        private const val DAILY_LIMIT_PER_IP = 5
        private const val DAILY_LIMIT_GLOBAL = 20
        private const val SPAM_COOLDOWN_SECONDS = 5L
        private const val SPAM_CACHE_EXPIRY_MINUTES = 30L
    }

    fun getMessages(clientIp: String, authenticated: Boolean): List<GuestbookMessageResponse> {
        return guestbookRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, MAX_DISPLAY))
            .reversed()
            .map { message ->
                toResponse(message, authenticated || message.ipAddress == clientIp)
            }
    }

    fun createMessage(request: GuestbookCreateRequest, clientIp: String): GuestbookMessageResponse {
        checkSpamCooldown(clientIp)
        checkDailyLimits(clientIp)

        val message = guestbookRepository.save(
            GuestbookMessage(
                nickname = request.nickname?.trim()?.takeIf { it.isNotBlank() },
                content = request.content.trim(),
                ipAddress = clientIp
            )
        )

        lastWriteTimeByIp[clientIp] = LocalDateTime.now()

        return toResponse(message, deletable = true)
    }

    fun deleteMessage(id: Long, clientIp: String, authenticated: Boolean) {
        val message = guestbookRepository.findById(id)
            .orElseThrow { NotFoundException("메시지를 찾을 수 없습니다.") }

        if (!authenticated && message.ipAddress != clientIp) {
            throw UnauthorizedException("삭제 권한이 없습니다.")
        }

        guestbookRepository.delete(message)
    }

    private fun checkSpamCooldown(clientIp: String) {
        val now = LocalDateTime.now()

        lastWriteTimeByIp.entries.removeIf { (_, time) ->
            time.plusMinutes(SPAM_CACHE_EXPIRY_MINUTES).isBefore(now)
        }

        val lastWrite = lastWriteTimeByIp[clientIp]
        if (lastWrite != null && lastWrite.plusSeconds(SPAM_COOLDOWN_SECONDS).isAfter(now)) {
            throw TooManyRequestsException("잠시 후 다시 시도해주세요.")
        }
    }

    private fun checkDailyLimits(clientIp: String) {
        val startOfDay = LocalDate.now().atStartOfDay()

        val ipCount = guestbookRepository.countByIpAddressAndCreatedAtAfter(clientIp, startOfDay)
        if (ipCount >= DAILY_LIMIT_PER_IP) {
            throw TooManyRequestsException("하루 최대 ${DAILY_LIMIT_PER_IP}개까지 작성할 수 있습니다.")
        }

        val globalCount = guestbookRepository.countAllIncludingDeletedAfter(startOfDay)
        if (globalCount >= DAILY_LIMIT_GLOBAL) {
            throw TooManyRequestsException("오늘의 방명록이 가득 찼습니다. 내일 다시 시도해주세요.")
        }
    }

    private fun toResponse(message: GuestbookMessage, deletable: Boolean) = GuestbookMessageResponse(
        id = message.id,
        nickname = message.nickname,
        content = message.content,
        createdAt = message.createdAt,
        deletable = deletable
    )
}
