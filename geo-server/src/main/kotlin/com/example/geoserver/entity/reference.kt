package com.example.geoserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "ref_areas")
data class RefArea(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String // Название участка
)

@Entity
@Table(name = "ref_contractors")
data class RefContractor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String // Подрядчик
)

@Entity
@Table(name = "ref_drilling_rigs")
data class RefDrillingRig(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String  // Номер буровой
)

@Entity
@Table(name = "ref_geologists")
data class RefGeologist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String  // ФИО геолога
)

@Entity
@Table(name = "ref_work_types")
data class RefWorkType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String  // "Скважина", "Шурф", "Расчистка"
)