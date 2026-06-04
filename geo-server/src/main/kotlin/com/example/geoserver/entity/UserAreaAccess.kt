package com.example.geoserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "user_area_access")
data class UserAreaAccess(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "area_id", nullable = false)
    val areaId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    val accessLevel: AccessLevel
)
