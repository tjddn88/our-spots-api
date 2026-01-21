package com.mrseong.picks.api.dto

import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType

data class MarkerResponse(
    val id: Long,
    val name: String,
    val type: PlaceType,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun from(place: Place) = MarkerResponse(
            id = place.id,
            name = place.name,
            type = place.type,
            latitude = place.latitude,
            longitude = place.longitude
        )
    }
}
