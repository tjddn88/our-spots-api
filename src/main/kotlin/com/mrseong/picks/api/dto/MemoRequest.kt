package com.mrseong.picks.api.dto

import com.mrseong.picks.domain.memo.entity.Rating
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class MemoCreateRequest(
    @field:NotBlank
    val itemName: String,

    @field:NotNull
    val rating: Rating,

    val comment: String? = null,

    val password: String? = null
)

data class MemoUpdateRequest(
    val itemName: String? = null,
    val rating: Rating? = null,
    val comment: String? = null,
    val password: String? = null
)

data class MemoDeleteRequest(
    val password: String? = null
)
