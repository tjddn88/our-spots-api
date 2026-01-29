package com.mrseong.picks.domain.place.controller

import com.mrseong.picks.api.dto.*
import com.mrseong.picks.common.response.ApiResponse
import com.mrseong.picks.domain.auth.service.AuthService
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.service.PlaceService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/places")
class PlaceController(
    private val placeService: PlaceService,
    private val authService: AuthService
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
        @Valid @RequestBody request: PlaceCreateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<PlaceResponse> {
        authService.validatePassword(
            password = request.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "POST /api/places"
        )
        return ApiResponse.success(placeService.createPlace(request))
    }

    @PutMapping("/{id}")
    fun updatePlace(
        @PathVariable id: Long,
        @Valid @RequestBody request: PlaceUpdateRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<PlaceResponse> {
        authService.validatePassword(
            password = request.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "PUT /api/places/$id"
        )
        return ApiResponse.success(placeService.updatePlace(id, request))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlace(
        @PathVariable id: Long,
        @RequestBody(required = false) request: DeleteRequest?,
        httpRequest: HttpServletRequest
    ) {
        authService.validatePassword(
            password = request?.password,
            ipAddress = getClientIp(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            endpoint = "DELETE /api/places/$id"
        )
        placeService.deletePlace(id)
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
