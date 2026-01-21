package com.mrseong.picks.api.dto

import com.mrseong.picks.domain.memo.entity.Memo
import com.mrseong.picks.domain.memo.entity.Rating
import java.time.LocalDateTime

data class MemoResponse(
    val id: Long,
    val placeId: Long,
    val itemName: String,
    val rating: Rating,
    val comment: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(memo: Memo) = MemoResponse(
            id = memo.id,
            placeId = memo.place.id,
            itemName = memo.itemName,
            rating = memo.rating,
            comment = memo.comment,
            createdAt = memo.createdAt
        )
    }
}
