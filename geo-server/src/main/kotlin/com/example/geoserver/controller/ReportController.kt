package com.example.geoserver.controller

import com.example.geoserver.service.ReportDataService
import com.example.geoserver.dto.ReportDataDto
import com.example.geoserver.dto.ReportMetadata
import com.example.geoserver.dto.ReportRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportDataService: ReportDataService
) {

    @PostMapping("/data")
    fun getReportData(@RequestBody request: ReportRequest): ReportDataDto {
        return reportDataService.getReportData(request)
    }
}