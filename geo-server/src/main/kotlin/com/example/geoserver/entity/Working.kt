/*Таблица для хранения информации о скважинах, шурфах и расчистках.
Каждая запись соответствует одной скважине, шурфу или расчистке,
с указанием их планировочных и фактических координат, глубины, 
дат начала и окончания работ, ответственных геолога и подрядчика, 
а также дополнительной информации. Таблица связана с другими 
справочными таблицами для обеспечения целостности данных и удобства управления ими.*/

package com.example.geoserver.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.Instant

@Entity
@Table(name = "workings")
data class Working(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne @JoinColumn(name = "area_id")
    val area: RefArea? = null,

    @ManyToOne @JoinColumn(name = "work_type_id")
    val workType: RefWorkType? = null,

    @ManyToOne @JoinColumn(name = "planned_contractor_id")
    val plannedContractor: RefContractor? = null,

    @Column(name = "structure")
    val structure: String? = null,

    @Column(nullable = true)
    val number: String,

    @Column(name = "order_num") val orderNum: Int? = null,
    
    // Координаты
    val plannedX: Double? = null,
    val plannedY: Double? = null,
    val plannedDepth: Double? = null,

    val actualX: Double? = null,
    val actualY: Double? = null,
    val actualZ: Double? = null,
    val actualDepth: Double? = null,

    val deltaS: Double? = null, // Смещение от проекта

    @Column(name = "start_date") val startDate: LocalDate? = null,
    @Column(name = "end_date") val endDate: LocalDate? = null,

    @ManyToOne @JoinColumn(name = "geologist_id") val geologist: RefGeologist? = null,
    @ManyToOne @JoinColumn(name = "contractor_id") val contractor: RefContractor? = null,
    @ManyToOne @JoinColumn(name = "drilling_rig_id") val drillingRig: RefDrillingRig? = null,

    @Column(name = "additional_info", columnDefinition = "TEXT")
    val additionalInfo: String? = null, // Комментарий

    @Column(name = "created_at") val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") val updatedAt: Instant? = null,

    @Column(name = "core_recovery") val coreRecovery: Double? = null,
    val casing: Double? = null,

    @Column(name = "mmg1_top") val mmg1Top: Double? = null,
    @Column(name = "mmg1_bottom") val mmg1Bottom: Double? = null,
    @Column(name = "mmg2_top") val mmg2Top: Double? = null,
    @Column(name = "mmg2_bottom") val mmg2Bottom: Double? = null,

    @Column(name = "gw_appear_log") val gwAppearLog: Double? = null, // ПУГВ
    @Column(name = "gw_stable_log") val gwStableLog: Double? = null, // УУГВ
    @Column(name = "gw_stable_abs") val gwStableAbs: Double? = null, // УУГВ абс (расчетное)

    // Чекбоксы (Акт и Термотрубка)
    val act: Boolean = false,
    @Column(name = "act_number") val actNumber: String? = null,
    @Column(name = "thermal_tube") val thermalTube: Boolean = false,

    // чекбоксы наличия материалов/журналов
    @Column (name = "has_video") val hasVideo: Boolean = false,
    @Column (name = "has_drilling") val hasDrilling: Boolean = false,
    @Column (name = "has_journal") val hasJournal: Boolean = false,
    @Column (name = "has_core") val hasCore: Boolean = false,
    @Column (name = "has_stake") val hasStake: Boolean = false,

    // образцы
    val samplesThawed: Int? = null,
    val samplesFrozen: Int? = null,
    val samplesRocky: Int? = null,

    // Флаг для определения проектной скважины
    val isProject: Boolean = false,

    // Категории по глубинам (для отчетов)
    @Column(name = "cat1_4") val cat1_4: Double? = null,
    @Column(name = "cat5_8") val cat5_8: Double? = null,
    @Column(name = "cat9_12") val cat9_12: Double? = null,
)