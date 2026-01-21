package com.mrseong.picks.domain.memo.service

import com.mrseong.picks.api.dto.MemoCreateRequest
import com.mrseong.picks.api.dto.MemoResponse
import com.mrseong.picks.api.dto.MemoUpdateRequest
import com.mrseong.picks.common.exception.NotFoundException
import com.mrseong.picks.domain.memo.entity.Memo
import com.mrseong.picks.domain.memo.repository.MemoRepository
import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemoService(
    private val memoRepository: MemoRepository,
    private val placeRepository: PlaceRepository
) {

    fun getMemosByPlace(placeId: Long): List<MemoResponse> {
        if (!placeRepository.existsById(placeId)) {
            throw NotFoundException("Place not found: $placeId")
        }
        return memoRepository.findByPlaceId(placeId).map { MemoResponse.from(it) }
    }

    @Transactional
    fun createMemo(placeId: Long, request: MemoCreateRequest): MemoResponse {
        val place = placeRepository.findById(placeId)
            .orElseThrow { NotFoundException("Place not found: $placeId") }

        val memo = Memo(
            place = place,
            itemName = request.itemName,
            rating = request.rating,
            comment = request.comment
        )
        return MemoResponse.from(memoRepository.save(memo))
    }

    @Transactional
    fun updateMemo(id: Long, request: MemoUpdateRequest): MemoResponse {
        val memo = memoRepository.findById(id)
            .orElseThrow { NotFoundException("Memo not found: $id") }

        request.itemName?.let { memo.itemName = it }
        request.rating?.let { memo.rating = it }
        request.comment?.let { memo.comment = it }

        return MemoResponse.from(memoRepository.save(memo))
    }

    @Transactional
    fun deleteMemo(id: Long) {
        if (!memoRepository.existsById(id)) {
            throw NotFoundException("Memo not found: $id")
        }
        memoRepository.deleteById(id)
    }
}
