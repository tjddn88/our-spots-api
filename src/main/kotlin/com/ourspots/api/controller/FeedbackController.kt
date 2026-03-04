package com.ourspots.api.controller

import com.ourspots.api.dto.FeedbackCreateRequest
import com.ourspots.common.response.ApiResponse
import com.ourspots.common.util.RequestUtils
import com.ourspots.domain.feedback.service.FeedbackService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    @PostMapping
    fun createFeedback(
        @Valid @RequestBody request: FeedbackCreateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<Unit> {
        val clientIp = RequestUtils.getClientIp(httpRequest)
        feedbackService.createFeedback(request, clientIp)
        return ApiResponse.success(Unit)
    }
}
