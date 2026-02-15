package com.ourspots.domain.guestbook.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ourspots.api.dto.GuestbookCreateRequest
import com.ourspots.domain.guestbook.repository.GuestbookRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestbookControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var guestbookRepository: GuestbookRepository

    @BeforeEach
    fun setUp() {
        guestbookRepository.deleteAll()
    }

    private fun loginAndGetToken(): String {
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"test-admin-password"}""")
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        return body["data"]["token"].asText()
    }

    private fun createMessage(content: String, nickname: String? = null): Long {
        val request = GuestbookCreateRequest(nickname = nickname, content = content)
        val result = mockMvc.perform(
            post("/api/guestbook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        return body["data"]["id"].asLong()
    }

    @Nested
    @DisplayName("POST /api/guestbook")
    inner class Create {

        @Test
        fun create_shouldReturnCreatedMessage() {
            val request = GuestbookCreateRequest(nickname = "방문자", content = "반갑습니다!")

            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("방문자"))
                .andExpect(jsonPath("$.data.content").value("반갑습니다!"))
                .andExpect(jsonPath("$.data.deletable").value(true))
                .andExpect(jsonPath("$.data.id").isNumber)
                .andExpect(jsonPath("$.data.createdAt").exists())
        }

        @Test
        fun create_whenNoNickname_shouldReturnNullNickname() {
            val request = GuestbookCreateRequest(content = "닉네임 없이")

            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.nickname").doesNotExist())
        }

        @Test
        fun create_whenBlankContent_shouldReturn400() {
            val request = GuestbookCreateRequest(content = "  ")

            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun create_whenContentExceeds200_shouldReturn400() {
            val request = GuestbookCreateRequest(content = "가".repeat(201))

            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun create_whenNicknameExceeds20_shouldReturn400() {
            val request = GuestbookCreateRequest(nickname = "가".repeat(21), content = "메시지")

            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun create_whenInvalidJson_shouldReturn400() {
            mockMvc.perform(
                post("/api/guestbook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/guestbook")
    inner class GetMessages {

        @Test
        fun getMessages_shouldReturnMessages() {
            createMessage("테스트 메시지", "테스터")

            mockMvc.perform(get("/api/guestbook"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("테스트 메시지"))
                .andExpect(jsonPath("$.data[0].nickname").value("테스터"))
                .andExpect(jsonPath("$.data[0].deletable").value(true))
        }

        @Test
        fun getMessages_whenEmpty_shouldReturnEmptyList() {
            mockMvc.perform(get("/api/guestbook"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0))
        }

        @Test
        fun getMessages_whenAuthenticated_shouldSetAllDeletable() {
            val token = loginAndGetToken()
            createMessage("메시지")

            mockMvc.perform(
                get("/api/guestbook")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].deletable").value(true))
        }
    }

    @Nested
    @DisplayName("DELETE /api/guestbook/{id}")
    inner class Delete {

        @Test
        fun delete_whenSameIp_shouldReturn204() {
            val messageId = createMessage("삭제할 메시지")

            mockMvc.perform(delete("/api/guestbook/$messageId"))
                .andExpect(status().isNoContent)

            // 삭제 후 조회 시 없어야 함
            mockMvc.perform(get("/api/guestbook"))
                .andExpect(jsonPath("$.data.length()").value(0))
        }

        @Test
        fun delete_whenAdmin_shouldReturn204() {
            val token = loginAndGetToken()
            val messageId = createMessage("관리자가 삭제")

            mockMvc.perform(
                delete("/api/guestbook/$messageId")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isNoContent)
        }

        @Test
        fun delete_whenNotFound_shouldReturn404() {
            mockMvc.perform(delete("/api/guestbook/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
