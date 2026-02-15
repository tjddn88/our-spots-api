package com.ourspots.domain.auth.controller

import com.ourspots.common.response.ApiResponse
import com.ourspots.common.util.RequestUtils
import com.ourspots.domain.auth.service.AuthService
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
            ipAddress = RequestUtils.getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent")
        )
        return ApiResponse.success(LoginResponse(token))
    }
}
