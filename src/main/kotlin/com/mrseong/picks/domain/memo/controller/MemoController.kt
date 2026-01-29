package com.mrseong.picks.domain.memo.controller

import com.mrseong.picks.api.dto.MemoCreateRequest
import com.mrseong.picks.api.dto.MemoDeleteRequest
import com.mrseong.picks.api.dto.MemoResponse
import com.mrseong.picks.api.dto.MemoUpdateRequest
import com.mrseong.picks.common.response.ApiResponse
import com.mrseong.picks.domain.auth.service.AuthService
import com.mrseong.picks.domain.memo.service.MemoService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class MemoController(
    private val memoService: MemoService,
    private val authService: AuthService
) {

    @GetMapping("/places/{placeId}/memos")
    fun getMemosByPlace(@PathVariable placeId: Long): ApiResponse<List<MemoResponse>> {
        return ApiResponse.success(memoService.getMemosByPlace(placeId))
    }

    @PostMapping("/places/{placeId}/memos")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMemo(
        @PathVariable placeId: Long,
        @Valid @RequestBody request: MemoCreateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<MemoResponse> {
        authService.validatePassword(
            password = request.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "POST /api/places/$placeId/memos"
        )
        return ApiResponse.success(memoService.createMemo(placeId, request))
    }

    @PutMapping("/memos/{id}")
    fun updateMemo(
        @PathVariable id: Long,
        @Valid @RequestBody request: MemoUpdateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<MemoResponse> {
        authService.validatePassword(
            password = request.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "PUT /api/memos/$id"
        )
        return ApiResponse.success(memoService.updateMemo(id, request))
    }

    @DeleteMapping("/memos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMemo(
        @PathVariable id: Long,
        @RequestBody(required = false) request: MemoDeleteRequest?,
        httpRequest: HttpServletRequest
    ) {
        authService.validatePassword(
            password = request?.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "DELETE /api/memos/$id"
        )
        memoService.deleteMemo(id)
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
