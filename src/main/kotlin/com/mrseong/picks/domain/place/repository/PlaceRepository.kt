package com.mrseong.picks.domain.place.repository

import com.mrseong.picks.domain.place.entity.Place
import com.mrseong.picks.domain.place.entity.PlaceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PlaceRepository : JpaRepository<Place, Long> {

    fun findByType(type: PlaceType): List<Place>

    @Query("""
        SELECT p FROM Place p
        WHERE p.latitude BETWEEN :swLat AND :neLat
        AND p.longitude BETWEEN :swLng AND :neLng
    """)
    fun findWithinBounds(swLat: Double, swLng: Double, neLat: Double, neLng: Double): List<Place>

    @Query("""
        SELECT p FROM Place p
        WHERE p.type = :type
        AND p.latitude BETWEEN :swLat AND :neLat
        AND p.longitude BETWEEN :swLng AND :neLng
    """)
    fun findByTypeWithinBounds(
        type: PlaceType,
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double
    ): List<Place>
}
