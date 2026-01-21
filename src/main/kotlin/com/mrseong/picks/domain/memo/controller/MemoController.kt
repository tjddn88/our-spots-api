package com.mrseong.picks.domain.memo.controller

import com.mrseong.picks.api.dto.MemoCreateRequest
import com.mrseong.picks.api.dto.MemoResponse
import com.mrseong.picks.api.dto.MemoUpdateRequest
import com.mrseong.picks.common.response.ApiResponse
import com.mrseong.picks.domain.memo.service.MemoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class MemoController(
    private val memoService: MemoService
) {

    @GetMapping("/places/{placeId}/memos")
    fun getMemosByPlace(@PathVariable placeId: Long): ApiResponse<List<MemoResponse>> {
        return ApiResponse.success(memoService.getMemosByPlace(placeId))
    }

    @PostMapping("/places/{placeId}/memos")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMemo(
        @PathVariable placeId: Long,
        @Valid @RequestBody request: MemoCreateRequest
    ): ApiResponse<MemoResponse> {
        return ApiResponse.success(memoService.createMemo(placeId, request))
    }

    @PutMapping("/memos/{id}")
    fun updateMemo(
        @PathVariable id: Long,
        @Valid @RequestBody request: MemoUpdateRequest
    ): ApiResponse<MemoResponse> {
        return ApiResponse.success(memoService.updateMemo(id, request))
    }

    @DeleteMapping("/memos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMemo(@PathVariable id: Long) {
        memoService.deleteMemo(id)
    }
}
