package com.example.geoserver.service

import com.example.geoserver.dto.ReportDataDto
import com.example.geoserver.dto.ReportMetadata
import com.example.geoserver.dto.ReportRequest
import com.example.geoserver.dto.ReportRowDto
import com.example.geoserver.repository.WorkingRepository
import com.example.geoserver.repository.WorkingSpecifications
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportDataService(
    private val workingRepository: WorkingRepository
) {

    fun getReportData(request: ReportRequest): ReportDataDto {
        /**
         * Использует Specification API для динамической фильтрации.
         * Это решает проблему SQLState: 42P18 в PostgreSQL при использовании ":param IS NULL".
         */
        val specification = WorkingSpecifications.filterByParameters(
            startDate = request.reportStart?.let { LocalDate.parse(it) },
            endDate = request.reportEnd?.let { LocalDate.parse(it) },
            contractorId = request.contractorId,
            areaId = request.areaId
        )
        

        val workings = workingRepository.findAll(specification)

        val rows = workings.map { working ->
            ReportRowDto(
                boreholeName = working.number,
                hValue = working.actualDepth,
                xCoord = working.actualX,
                yCoord = working.actualY,
                zCoord = working.actualZ,
                startDate = working.startDate?.toString(),
                endDate = working.endDate?.toString(),
                geologistName = working.geologist?.name
            )
        }

        return ReportDataDto(
            rows = rows,
            metadata = ReportMetadata(
                totalRows = rows.size,
                summary = mapOf(
                    "totalDepth" to rows.sumOf { it.hValue ?: 0.0 },
                    "count" to rows.size
                ),
                appliedFilters = mapOf(
                    "period" to "${request.reportStart} - ${request.reportEnd}",
                    "contractorId" to request.contractorId,
                    "areaId" to request.areaId
                )
            )
        )
    }
}