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
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "workings")
@Audited
@SQLDelete(sql = "UPDATE workings SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted = false")
class Working(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "area_id")
    val area: RefArea? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "work_type_id")
    val workType: RefWorkType? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "planned_contractor_id")
    val plannedContractor: RefContractor? = null,

    @Column(name = "structure")
    val structure: String? = null,

    @Column(nullable = false)
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

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "geologist_id")
    val geologist: RefGeologist? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "contractor_id")
    val contractor: RefContractor? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "drilling_rig_id")
    val drillingRig: RefDrillingRig? = null,

    @Column(name = "additional_info", columnDefinition = "TEXT")
    val additionalInfo: String? = null, // Комментарий

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: String? = null,

    @LastModifiedBy
    @Column(name = "updated_by")
    var updatedBy: String? = null,

    @CreatedDate
    @Column(name = "audit_created_at", updatable = false)
    var auditCreatedAt: Instant? = null,

    @LastModifiedDate
    @Column(name = "audit_updated_at")
    var auditUpdatedAt: Instant? = null,


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
    @Column(name = "has_video") val hasVideo: Boolean = false,
    @Column(name = "has_drilling") val hasDrilling: Boolean = false,
    @Column(name = "has_journal") val hasJournal: Boolean = false,
    @Column(name = "has_core") val hasCore: Boolean = false,
    @Column(name = "has_stake") val hasStake: Boolean = false,

    @Column(name = "emergency") val emergency: Boolean = false, // Аварийная скважина


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


    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
) {
    fun copy(
        id: Long = this.id,
        area: RefArea? = this.area,
        workType: RefWorkType? = this.workType,
        plannedContractor: RefContractor? = this.plannedContractor,
        structure: String? = this.structure,
        number: String = this.number,
        orderNum: Int? = this.orderNum,
        plannedX: Double? = this.plannedX,
        plannedY: Double? = this.plannedY,
        plannedDepth: Double? = this.plannedDepth,
        actualX: Double? = this.actualX,
        actualY: Double? = this.actualY,
        actualZ: Double? = this.actualZ,
        actualDepth: Double? = this.actualDepth,
        deltaS: Double? = this.deltaS,
        startDate: LocalDate? = this.startDate,
        endDate: LocalDate? = this.endDate,
        geologist: RefGeologist? = this.geologist,
        contractor: RefContractor? = this.contractor,
        drillingRig: RefDrillingRig? = this.drillingRig,
        additionalInfo: String? = this.additionalInfo,
        createdBy: String? = this.createdBy,
        updatedBy: String? = this.updatedBy,
        auditCreatedAt: Instant? = this.auditCreatedAt,
        auditUpdatedAt: Instant? = this.auditUpdatedAt,
        coreRecovery: Double? = this.coreRecovery,
        casing: Double? = this.casing,
        mmg1Top: Double? = this.mmg1Top,
        mmg1Bottom: Double? = this.mmg1Bottom,
        mmg2Top: Double? = this.mmg2Top,
        mmg2Bottom: Double? = this.mmg2Bottom,
        gwAppearLog: Double? = this.gwAppearLog,
        gwStableLog: Double? = this.gwStableLog,
        gwStableAbs: Double? = this.gwStableAbs,
        act: Boolean = this.act,
        actNumber: String? = this.actNumber,
        thermalTube: Boolean = this.thermalTube,
        hasVideo: Boolean = this.hasVideo,
        hasDrilling: Boolean = this.hasDrilling,
        hasJournal: Boolean = this.hasJournal,
        hasCore: Boolean = this.hasCore,
        hasStake: Boolean = this.hasStake,
        emergency: Boolean = this.emergency,
        samplesThawed: Int? = this.samplesThawed,
        samplesFrozen: Int? = this.samplesFrozen,
        samplesRocky: Int? = this.samplesRocky,
        isProject: Boolean = this.isProject,
        cat1_4: Double? = this.cat1_4,
        cat5_8: Double? = this.cat5_8,
        cat9_12: Double? = this.cat9_12,
        isDeleted: Boolean = this.isDeleted
    ) = Working(
        id = id,
        area = area,
        workType = workType,
        plannedContractor = plannedContractor,
        structure = structure,
        number = number,
        orderNum = orderNum,
        plannedX = plannedX,
        plannedY = plannedY,
        plannedDepth = plannedDepth,
        actualX = actualX,
        actualY = actualY,
        actualZ = actualZ,
        actualDepth = actualDepth,
        deltaS = deltaS,
        startDate = startDate,
        endDate = endDate,
        geologist = geologist,
        contractor = contractor,
        drillingRig = drillingRig,
        additionalInfo = additionalInfo,
        createdBy = createdBy,
        updatedBy = updatedBy,
        auditCreatedAt = auditCreatedAt,
        auditUpdatedAt = auditUpdatedAt,
        coreRecovery = coreRecovery,
        casing = casing,
        mmg1Top = mmg1Top,
        mmg1Bottom = mmg1Bottom,
        mmg2Top = mmg2Top,
        mmg2Bottom = mmg2Bottom,
        gwAppearLog = gwAppearLog,
        gwStableLog = gwStableLog,
        gwStableAbs = gwStableAbs,
        act = act,
        actNumber = actNumber,
        thermalTube = thermalTube,
        hasVideo = hasVideo,
        hasDrilling = hasDrilling,
        hasJournal = hasJournal,
        hasCore = hasCore,
        hasStake = hasStake,
        emergency = emergency,
        samplesThawed = samplesThawed,
        samplesFrozen = samplesFrozen,
        samplesRocky = samplesRocky,
        isProject = isProject,
        cat1_4 = cat1_4,
        cat5_8 = cat5_8,
        cat9_12 = cat9_12,
        isDeleted = isDeleted
    )
}