package com.ourspots.domain.guestbook.service

import com.ourspots.api.dto.GuestbookCreateRequest
import com.ourspots.common.exception.NotFoundException
import com.ourspots.common.exception.TooManyRequestsException
import com.ourspots.common.exception.UnauthorizedException
import com.ourspots.domain.guestbook.entity.GuestbookMessage
import com.ourspots.domain.guestbook.repository.GuestbookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuestbookServiceTest {

    private lateinit var guestbookRepository: GuestbookRepository
    private lateinit var guestbookService: GuestbookService

    private val testIp = "192.168.1.1"
    private val otherIp = "192.168.1.2"

    @BeforeEach
    fun setUp() {
        guestbookRepository = mockk()
        guestbookService = GuestbookService(guestbookRepository)
    }

    private fun stubCreateDefaults(ipCount: Long = 0, globalCount: Long = 0) {
        every { guestbookRepository.save(any<GuestbookMessage>()) } answers {
            val msg = firstArg<GuestbookMessage>()
            GuestbookMessage(
                id = 1, nickname = msg.nickname, content = msg.content, ipAddress = msg.ipAddress
            )
        }
        every { guestbookRepository.countByIpAddressAndCreatedAtAfter(any(), any<LocalDateTime>()) } returns ipCount
        every { guestbookRepository.countAllIncludingDeletedAfter(any<LocalDateTime>()) } returns globalCount
    }

    @Nested
    @DisplayName("getMessages")
    inner class GetMessages {

        @Test
        fun getMessages_whenSameIp_shouldSetDeletableTrue() {
            val message = GuestbookMessage(id = 1, nickname = "테스터", content = "안녕하세요", ipAddress = testIp)
            every { guestbookRepository.findByOrderByCreatedAtDesc(any<PageRequest>()) } returns listOf(message)

            val result = guestbookService.getMessages(testIp, false)

            assertEquals(1, result.size)
            assertTrue(result[0].deletable)
            assertEquals("테스터", result[0].nickname)
        }

        @Test
        fun getMessages_whenDifferentIp_shouldSetDeletableFalse() {
            val message = GuestbookMessage(id = 1, nickname = null, content = "안녕하세요", ipAddress = testIp)
            every { guestbookRepository.findByOrderByCreatedAtDesc(any<PageRequest>()) } returns listOf(message)

            val result = guestbookService.getMessages(otherIp, false)

            assertEquals(1, result.size)
            assertFalse(result[0].deletable)
        }

        @Test
        fun getMessages_whenAuthenticated_shouldSetAllDeletableTrue() {
            val message = GuestbookMessage(id = 1, nickname = null, content = "안녕하세요", ipAddress = otherIp)
            every { guestbookRepository.findByOrderByCreatedAtDesc(any<PageRequest>()) } returns listOf(message)

            val result = guestbookService.getMessages(testIp, true)

            assertTrue(result[0].deletable)
        }

        @Test
        fun getMessages_shouldReturnInChronologicalOrder() {
            val older = GuestbookMessage(id = 1, content = "첫 번째", ipAddress = testIp)
            val newer = GuestbookMessage(id = 2, content = "두 번째", ipAddress = testIp)
            every { guestbookRepository.findByOrderByCreatedAtDesc(any<PageRequest>()) } returns listOf(newer, older)

            val result = guestbookService.getMessages(testIp, false)

            assertEquals("첫 번째", result[0].content)
            assertEquals("두 번째", result[1].content)
        }

        @Test
        fun getMessages_whenEmpty_shouldReturnEmptyList() {
            every { guestbookRepository.findByOrderByCreatedAtDesc(any<PageRequest>()) } returns emptyList()

            val result = guestbookService.getMessages(testIp, false)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("createMessage")
    inner class CreateMessage {

        @Test
        fun createMessage_shouldSaveAndReturnMessage() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(nickname = "테스터", content = "안녕하세요")

            val result = guestbookService.createMessage(request, testIp)

            assertEquals(1, result.id)
            assertEquals("테스터", result.nickname)
            assertEquals("안녕하세요", result.content)
            assertTrue(result.deletable)
        }

        @Test
        fun createMessage_whenNullNickname_shouldSaveAsNull() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(nickname = null, content = "테스트")

            val result = guestbookService.createMessage(request, testIp)

            assertNull(result.nickname)
        }

        @Test
        fun createMessage_whenBlankNickname_shouldSaveAsNull() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(nickname = "  ", content = "테스트")

            val result = guestbookService.createMessage(request, testIp)

            assertNull(result.nickname)
        }

        @Test
        fun createMessage_shouldTrimContent() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(content = "  메시지  ")

            val result = guestbookService.createMessage(request, testIp)

            assertEquals("메시지", result.content)
        }

        @Test
        fun createMessage_whenSpamming_shouldThrowTooManyRequests() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(content = "메시지")

            guestbookService.createMessage(request, testIp)

            assertThrows<TooManyRequestsException> {
                guestbookService.createMessage(request, testIp)
            }
        }

        @Test
        fun createMessage_whenDifferentIp_shouldNotBlockSpam() {
            stubCreateDefaults()
            val request = GuestbookCreateRequest(content = "메시지")

            guestbookService.createMessage(request, testIp)
            guestbookService.createMessage(request, otherIp)
        }

        @Test
        fun createMessage_whenIpDailyLimitReached_shouldThrowTooManyRequests() {
            stubCreateDefaults(ipCount = 5)
            val request = GuestbookCreateRequest(content = "메시지")

            val exception = assertThrows<TooManyRequestsException> {
                guestbookService.createMessage(request, testIp)
            }
            assertTrue(exception.message!!.contains("5개"))
        }

        @Test
        fun createMessage_whenGlobalDailyLimitReached_shouldThrowTooManyRequests() {
            stubCreateDefaults(globalCount = 20)
            val request = GuestbookCreateRequest(content = "메시지")

            val exception = assertThrows<TooManyRequestsException> {
                guestbookService.createMessage(request, testIp)
            }
            assertTrue(exception.message!!.contains("가득"))
        }

        @Test
        fun createMessage_whenIpUnderLimit_shouldSucceed() {
            stubCreateDefaults(ipCount = 4, globalCount = 19)
            val request = GuestbookCreateRequest(content = "메시지")

            val result = guestbookService.createMessage(request, testIp)

            assertEquals("메시지", result.content)
        }
    }

    @Nested
    @DisplayName("deleteMessage")
    inner class DeleteMessage {

        @Test
        fun deleteMessage_whenSameIp_shouldDelete() {
            val message = GuestbookMessage(id = 1, content = "테스트", ipAddress = testIp)
            every { guestbookRepository.findById(1L) } returns Optional.of(message)
            every { guestbookRepository.delete(message) } returns Unit

            guestbookService.deleteMessage(1L, testIp, false)

            verify { guestbookRepository.delete(message) }
        }

        @Test
        fun deleteMessage_whenAuthenticated_shouldDeleteAnyMessage() {
            val message = GuestbookMessage(id = 1, content = "테스트", ipAddress = otherIp)
            every { guestbookRepository.findById(1L) } returns Optional.of(message)
            every { guestbookRepository.delete(message) } returns Unit

            guestbookService.deleteMessage(1L, testIp, true)

            verify { guestbookRepository.delete(message) }
        }

        @Test
        fun deleteMessage_whenDifferentIpAndNotAuthenticated_shouldThrowUnauthorized() {
            val message = GuestbookMessage(id = 1, content = "테스트", ipAddress = otherIp)
            every { guestbookRepository.findById(1L) } returns Optional.of(message)

            assertThrows<UnauthorizedException> {
                guestbookService.deleteMessage(1L, testIp, false)
            }
        }

        @Test
        fun deleteMessage_whenNotFound_shouldThrowNotFoundException() {
            every { guestbookRepository.findById(1L) } returns Optional.empty()

            assertThrows<NotFoundException> {
                guestbookService.deleteMessage(1L, testIp, false)
            }
        }
    }
}
