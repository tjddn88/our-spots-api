package com.ourspots.api.controller

import com.ourspots.api.dto.GuestbookCreateRequest
import com.ourspots.api.dto.GuestbookMessageResponse
import com.ourspots.common.response.ApiResponse
import com.ourspots.common.util.RequestUtils
import com.ourspots.domain.auth.service.JwtProvider
import com.ourspots.domain.guestbook.service.GuestbookService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/guestbook")
class GuestbookController(
    private val guestbookService: GuestbookService,
    private val jwtProvider: JwtProvider
) {

    @GetMapping
    fun getMessages(
        @RequestHeader("Authorization", required = false) authHeader: String?,
        httpRequest: HttpServletRequest
    ): ApiResponse<List<GuestbookMessageResponse>> {
        val clientIp = RequestUtils.getClientIp(httpRequest)
        val authenticated = jwtProvider.isValidAuthHeader(authHeader)
        return ApiResponse.success(guestbookService.getMessages(clientIp, authenticated))
    }

    @PostMapping
    fun createMessage(
        @Valid @RequestBody request: GuestbookCreateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<GuestbookMessageResponse> {
        val clientIp = RequestUtils.getClientIp(httpRequest)
        return ApiResponse.success(guestbookService.createMessage(request, clientIp))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMessage(
        @PathVariable id: Long,
        @RequestHeader("Authorization", required = false) authHeader: String?,
        httpRequest: HttpServletRequest
    ) {
        val clientIp = RequestUtils.getClientIp(httpRequest)
        val authenticated = jwtProvider.isValidAuthHeader(authHeader)
        guestbookService.deleteMessage(id, clientIp, authenticated)
    }
}
