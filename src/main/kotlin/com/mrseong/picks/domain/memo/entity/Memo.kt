package com.mrseong.picks.domain.memo.entity

import com.mrseong.picks.domain.place.entity.Place
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "memos")
class Memo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    var place: Place,

    @Column(nullable = false)
    var itemName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rating: Rating,

    var comment: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
