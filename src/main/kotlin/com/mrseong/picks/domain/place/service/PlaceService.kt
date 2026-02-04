package com.mrseong.picks.domain.place.service

import com.mrseong.picks.api.dto.*
import com.mrseong.picks.common.exception.DuplicateException
import com.mrseong.picks.common.exception.NotFoundException
import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val placeRepository: PlaceRepository
) {

    fun getAllPlaces(type: PlaceType?): List<PlaceResponse> {
        val places = if (type != null) {
            placeRepository.findByType(type)
        } else {
            placeRepository.findAll()
        }
        return places.map { PlaceResponse.from(it) }
    }

    fun getPlace(id: Long): PlaceDetailResponse {
        val place = placeRepository.findById(id)
            .orElseThrow { NotFoundException("Place not found: $id") }
        return PlaceDetailResponse.from(place)
    }

    @Transactional
    fun createPlace(request: PlaceCreateRequest): PlaceResponse {
        // 이름+주소 중복 체크
        if (placeRepository.existsByNameAndAddress(request.name, request.address)) {
            throw DuplicateException("동일한 이름으로 이미 등록된 주소입니다: ${request.name}")
        }

        val place = Place(
            name = request.name,
            type = request.type,
            address = request.address,
            latitude = request.latitude,
            longitude = request.longitude,
            description = request.description,
            imageUrl = request.imageUrl,
            grade = request.grade
        )
        return PlaceResponse.from(placeRepository.save(place))
    }

    @Transactional
    fun updatePlace(id: Long, request: PlaceUpdateRequest): PlaceResponse {
        val place = placeRepository.findById(id)
            .orElseThrow { NotFoundException("Place not found: $id") }

        request.name?.let { place.name = it }
        request.type?.let { place.type = it }
        request.address?.let { place.address = it }
        request.latitude?.let { place.latitude = it }
        request.longitude?.let { place.longitude = it }
        request.description?.let { place.description = it }
        request.imageUrl?.let { place.imageUrl = it }
        request.grade?.let { place.grade = it }
        request.googlePlaceId?.let { place.googlePlaceId = it }
        request.googleRating?.let { place.googleRating = it }
        request.googleRatingsTotal?.let { place.googleRatingsTotal = it }

        return PlaceResponse.from(placeRepository.save(place))
    }

    @Transactional
    fun deletePlace(id: Long) {
        val place = placeRepository.findById(id)
            .orElseThrow { NotFoundException("Place not found: $id") }
        // Soft Delete: @SQLDelete 어노테이션에 의해 deletedAt이 설정됨
        placeRepository.delete(place)
    }

    @CacheEvict(value = ["markers"], allEntries = true)
    fun evictMarkersCache() {
        // 캐시만 비움
    }

    @Cacheable(value = ["markers"], key = "'markers:' + #type + ':' + #swLat + ':' + #swLng + ':' + #neLat + ':' + #neLng")
    fun getMarkers(
        type: PlaceType?,
        swLat: Double?,
        swLng: Double?,
        neLat: Double?,
        neLng: Double?
    ): List<MarkerResponse> {
        val places = when {
            swLat != null && swLng != null && neLat != null && neLng != null -> {
                if (type != null) {
                    placeRepository.findByTypeWithinBounds(type, swLat, swLng, neLat, neLng)
                } else {
                    placeRepository.findWithinBounds(swLat, swLng, neLat, neLng)
                }
            }
            type != null -> placeRepository.findByType(type)
            else -> placeRepository.findAll()
        }
        return places.map { MarkerResponse.from(it) }
    }
}
