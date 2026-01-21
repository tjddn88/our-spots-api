package com.mrseong.picks.domain.place.service

import com.mrseong.picks.api.dto.*
import com.mrseong.picks.common.exception.NotFoundException
import com.mrseong.picks.domain.memo.repository.MemoRepository
import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import com.mrseong.picks.domain.place.repository.PlaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val placeRepository: PlaceRepository,
    private val memoRepository: MemoRepository
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
        val memos = memoRepository.findByPlaceId(id).map { MemoResponse.from(it) }
        return PlaceDetailResponse.from(place, memos)
    }

    @Transactional
    fun createPlace(request: PlaceCreateRequest): PlaceResponse {
        val place = Place(
            name = request.name,
            type = request.type,
            address = request.address,
            latitude = request.latitude,
            longitude = request.longitude,
            description = request.description,
            imageUrl = request.imageUrl
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

        return PlaceResponse.from(placeRepository.save(place))
    }

    @Transactional
    fun deletePlace(id: Long) {
        if (!placeRepository.existsById(id)) {
            throw NotFoundException("Place not found: $id")
        }
        memoRepository.deleteByPlaceId(id)
        placeRepository.deleteById(id)
    }

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
