package com.mrseong.picks.api.dto

import com.mrseong.picks.domain.place.entity.PlaceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PlaceCreateRequest(
    @field:NotBlank
    val name: String,

    @field:NotNull
    val type: PlaceType,

    @field:NotBlank
    val address: String,

    @field:NotNull
    val latitude: Double,

    @field:NotNull
    val longitude: Double,

    val description: String? = null,

    val imageUrl: String? = null,

    val grade: Int? = null
)

data class PlaceUpdateRequest(
    val name: String? = null,
    val type: PlaceType? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val grade: Int? = null,
    val googlePlaceId: String? = null,
    val googleRating: Double? = null,
    val googleRatingsTotal: Int? = null
)
