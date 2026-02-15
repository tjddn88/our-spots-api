package com.ourspots.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class GuestbookCreateRequest(
    @field:Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
    val nickname: String? = null,

    @field:NotBlank(message = "메시지를 입력해주세요.")
    @field:Size(max = 200, message = "메시지는 200자 이하여야 합니다.")
    val content: String = ""
)

data class GuestbookMessageResponse(
    val id: Long,
    val nickname: String?,
    val content: String,
    val createdAt: LocalDateTime,
    val deletable: Boolean
)
