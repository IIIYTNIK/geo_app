package com.example.geoserver.entity

import jakarta.persistence.*
import com.fasterxml.jackson.annotation.JsonIgnore

@Entity
@Table(name = "ref_areas")
data class RefArea(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String, // Название участка

    @Column(columnDefinition = "TEXT") 
    var comment: String? = null
)

@Entity
@Table(name = "ref_contractors")
data class RefContractor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
) {
    // связь один-ко-многим
    @JsonIgnore
    @OneToMany(mappedBy = "contractor", cascade = [CascadeType.ALL], orphanRemoval = true)
    val geologists: MutableList<RefGeologist> = mutableListOf()
}

@Entity
@Table(name = "ref_geologists")
data class RefGeologist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,

    // Обратная связь (ManyToOne)
    @ManyToOne
    @JoinColumn(name = "contractor_id", nullable = false)
    var contractor: RefContractor? = null,

    @Column(unique = false) // Имя геолога может повторяться у разных подрядчиков
    val alias: String? = null,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
)

@Entity
@Table(name = "ref_drilling_rigs")
data class RefDrillingRig(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,  // Номер буровой

    @Column(unique = false)
    val alias: String? = null,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
)

@Entity
@Table(name = "ref_work_types")
data class RefWorkType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,  // "Скважина", "Шурф", "Расчистка"

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
)