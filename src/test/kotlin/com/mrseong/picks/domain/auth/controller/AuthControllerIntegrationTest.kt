package com.mrseong.picks.domain.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val adminPassword = "test-admin-password"

    @Nested
    @DisplayName("POST /api/auth/login")
    inner class Login {

        @Test
        fun login_whenCorrectPassword_shouldReturnToken() {
            // given
            val request = LoginRequest(adminPassword)

            // when & then
            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
        }

        @Test
        fun login_whenWrongPassword_shouldReturn401() {
            // given
            val request = LoginRequest("wrong-password")

            // when & then
            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun login_whenTokenReturned_shouldBeUsableForAuthenticatedApi() {
            // given
            val loginRequest = LoginRequest(adminPassword)
            val loginResult = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(loginResult.response.contentAsString)
            val token = response.get("data").get("token").asText()

            // when & then
            mockMvc.perform(
                post("/api/map/markers/refresh")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isOk)
        }

        @Test
        fun authenticatedApi_whenInvalidToken_shouldReturn401() {
            mockMvc.perform(
                post("/api/map/markers/refresh")
                    .header("Authorization", "Bearer invalid-token")
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
