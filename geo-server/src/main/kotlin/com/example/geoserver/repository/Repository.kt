package com.example.geoserver.repository

import com.example.geoserver.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.time.LocalDate

interface ReportTemplateRepository : JpaRepository<ReportTemplate, Long> {
    fun findByName(name: String): Optional<ReportTemplate>
}

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
}

// Справочники
interface RefContractorRepository : JpaRepository<RefContractor, Long> {
    fun findByName(name: String): RefContractor?
}

interface RefAreaRepository : JpaRepository<RefArea, Long> {
    fun findByName(name: String): RefArea?
}

interface RefGeologistRepository : JpaRepository<RefGeologist, Long> {
    fun findByName(name: String): RefGeologist?
    fun findByContractorId(contractorId: Long): List<RefGeologist>
}

interface RefDrillingRigRepository : JpaRepository<RefDrillingRig, Long> {
    fun findByName(name: String): RefDrillingRig?
}

interface RefWorkTypeRepository : JpaRepository<RefWorkType, Long> {
    fun findByName(name: String): RefWorkType?
}

// Основная таблица выработок
interface WorkingRepository : JpaRepository<Working, Long> {
    fun findByNumber(number: String): Working?

    @Query("SELECT MAX(w.orderNum) FROM Working w")
    fun findMaxOrderNum(): Int?

    @Modifying
    @Query("UPDATE Working w SET w.orderNum = w.orderNum - 1 WHERE w.orderNum > :deletedOrderNum")
    fun shiftOrderNums(deletedOrderNum: Int)

    fun findByNumberAndAreaId(number: String, areaId: Long?): Working?

    @Query("""
        SELECT w FROM Working w
        WHERE (:start IS NULL OR w.startDate >= :start)
          AND (:end IS NULL OR w.endDate <= :end)
          AND (:contractorId IS NULL OR w.contractor.id = :contractorId)
          AND (:areaId IS NULL OR w.area.id = :areaId)
    """)
    fun findByFilters(
        start: LocalDate?,
        end: LocalDate?,
        contractorId: Long?,
        areaId: Long?
    ): List<Working>
}