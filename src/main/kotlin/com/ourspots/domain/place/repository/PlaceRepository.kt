package com.ourspots.domain.place.repository

import com.ourspots.domain.place.entity.Place
import com.ourspots.domain.place.entity.PlaceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PlaceRepository : JpaRepository<Place, Long> {

    fun findByType(type: PlaceType): List<Place>

    fun existsByNameAndAddress(name: String, address: String): Boolean

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

    @Query("""
        SELECT p FROM Place p
        WHERE p.googleRatingFailCount < :maxFailCount
        AND (
            p.googleRating IS NULL
            OR p.googleRatingUpdatedAt IS NULL
            OR p.googleRatingUpdatedAt < :cutoffDate
        )
    """)
    fun findPlacesEligibleForGoogleSync(
        maxFailCount: Int,
        cutoffDate: LocalDateTime
    ): List<Place>
}
