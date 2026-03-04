package com.ourspots.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FeedbackCreateRequest(
    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 500, message = "내용은 500자 이하여야 합니다.")
    val content: String = ""
)
