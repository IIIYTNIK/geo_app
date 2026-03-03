/*Таблица для хранения информации о скважинах, шурфах и расчистках.
Каждая запись соответствует одной скважине, шурфу или расчистке,
с указанием их планировочных и фактических координат, глубины, 
дат начала и окончания работ, ответственных геолога и подрядчика, 
а также дополнительной информации. Таблица связана с другими 
справочными таблицами для обеспечения целостности данных и удобства управления ими.*/

package com.example.geoserver.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "workings")
data class Working(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = true)
    val area: RefArea? = null,

    @ManyToOne
    @JoinColumn(name = "work_type_id", nullable = true)
    val workType: RefWorkType? = null,

    @Column(nullable = true, unique = true)
    val number: String,  // номер скважины/шурфа/расчистки

    // Планировочные координаты
    val plannedX: Double? = null,
    val plannedY: Double? = null,
    val plannedZ: Double? = null,

    // Фактические координаты
    val actualX: Double? = null,
    val actualY: Double? = null,
    val actualZ: Double? = null,

    val depth: Double? = null,  // глубина

    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @ManyToOne
    @JoinColumn(name = "geologist_id")
    val geologist: RefGeologist? = null,

    @ManyToOne
    @JoinColumn(name = "contractor_id")
    val contractor: RefContractor? = null,

    @ManyToOne
    @JoinColumn(name = "drilling_rig_id")
    val drillingRig: RefDrillingRig? = null,

    @Column(name = "additional_info", columnDefinition = "TEXT")
    val additionalInfo: String? = null,

    // Аудит
    @Column(name = "created_at")
    val createdAt: java.time.Instant = java.time.Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: java.time.Instant? = null
)