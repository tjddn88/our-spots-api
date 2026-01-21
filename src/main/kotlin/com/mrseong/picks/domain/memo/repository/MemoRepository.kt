package com.mrseong.picks.domain.memo.repository

import com.mrseong.picks.domain.memo.entity.Memo
import org.springframework.data.jpa.repository.JpaRepository

interface MemoRepository : JpaRepository<Memo, Long> {

    fun findByPlaceId(placeId: Long): List<Memo>

    fun deleteByPlaceId(placeId: Long)
}
