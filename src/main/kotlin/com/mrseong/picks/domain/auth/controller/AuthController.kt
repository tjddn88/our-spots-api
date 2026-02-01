package com.mrseong.picks.domain.auth.controller

import com.mrseong.picks.common.response.ApiResponse
import com.mrseong.picks.domain.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class LoginRequest(val password: String)
data class LoginResponse(val token: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<LoginResponse> {
        val token = authService.login(
            password = request.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent")
        )
        return ApiResponse.success(LoginResponse(token))
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
    }
}
