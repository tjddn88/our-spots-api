package com.mrseong.picks.domain.place.entity

import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "places")
@SQLDelete(sql = "UPDATE places SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class Place(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PlaceType,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    var description: String? = null,

    // 맛집 등급 (1=최고, 2=좋음, 3=보통)
    var grade: Int? = null,

    // Google Places API 연동
    var googlePlaceId: String? = null,
    var googleRating: Double? = null,
    var googleRatingsTotal: Int? = null,

    // === 미사용 필드 (DB 스키마 호환성을 위해 유지) ===
    @Deprecated("미사용 필드 - 프론트엔드에서 이미지 기능 제거됨")
    var imageUrl: String? = null,

    @Deprecated("미사용 필드 - 회원 기능 미구현")
    var userId: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Soft Delete
    var deletedAt: LocalDateTime? = null
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
