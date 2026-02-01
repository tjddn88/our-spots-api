package com.mrseong.picks.domain.place.controller

import com.mrseong.picks.api.dto.*
import com.mrseong.picks.common.response.ApiResponse
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.service.PlaceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/places")
class PlaceController(
    private val placeService: PlaceService
) {

    @GetMapping
    fun getAllPlaces(
        @RequestParam(required = false) type: PlaceType?
    ): ApiResponse<List<PlaceResponse>> {
        return ApiResponse.success(placeService.getAllPlaces(type))
    }

    @GetMapping("/{id}")
    fun getPlace(@PathVariable id: Long): ApiResponse<PlaceDetailResponse> {
        return ApiResponse.success(placeService.getPlace(id))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlace(
        @Valid @RequestBody request: PlaceCreateRequest
    ): ApiResponse<PlaceResponse> {
        return ApiResponse.success(placeService.createPlace(request))
    }

    @PutMapping("/{id}")
    fun updatePlace(
        @PathVariable id: Long,
        @Valid @RequestBody request: PlaceUpdateRequest
    ): ApiResponse<PlaceResponse> {
        return ApiResponse.success(placeService.updatePlace(id, request))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlace(@PathVariable id: Long) {
        placeService.deletePlace(id)
    }
}
