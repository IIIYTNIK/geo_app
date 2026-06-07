package com.example.geoserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "user_area_access")
class UserAreaAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    val area: RefArea,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val accessLevel: AccessLevel
)

enum class AccessLevel {
    READ,
    WRITE
}