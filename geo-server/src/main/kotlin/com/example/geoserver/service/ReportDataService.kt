package com.example.geoserver.service

import com.example.geoserver.dto.ReportDataDto
import com.example.geoserver.dto.ReportMetadata
import com.example.geoserver.dto.ReportRequest
import com.example.geoserver.repository.WorkingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportDataService(
    private val workingRepository: WorkingRepository
) {

    fun getReportData(request: ReportRequest): ReportDataDto {
        val workings = workingRepository.findByFilters(
            reportType = "drilling_completed",    
            startDate = request.reportStart,
            endDate = request.reportEnd,
            contractorId = request.contractorId,
            areaId = request.areaId
        )

        val rows = workings.map { working ->
            mapOf<String, Any?>(
                "number" to working.number,
                "H" to working.actualDepth,
                "X_plan" to working.plannedX,
                "Y_plan" to working.plannedY,
                "X_fact" to working.actualX,
                "Y_fact" to working.actualY,
                "Z_fact" to working.actualZ,
                "start_date" to working.startDate,
                "end_date" to working.endDate,
                "geologist" to working.geologist?.name,
                "contractor" to working.contractor?.name,
                "area" to working.area?.name,
                "cat1_4" to working.cat1_4,
                "cat5_8" to working.cat5_8,
                "cat9_12" to working.cat9_12
            )
        }

        return ReportDataDto(
            rows = rows,
            metadata = ReportMetadata(
                totalRows = rows.size,
                summary = mapOf(
                    "totalDepth" to rows.sumOf { (it["H"] as? Double?) ?: 0.0 },
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