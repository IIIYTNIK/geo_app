package com.example.geoserver.controller

import com.example.geoserver.dto.report.ReportGenerateRequest
import com.example.geoserver.service.UniversalReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: UniversalReportService
) {

    @PostMapping("/generate")
    fun generate(@RequestBody request: ReportGenerateRequest): ResponseEntity<ByteArray> {
        val bytes = reportService.generateGenericReport(
            templateId = request.templateId,
            params = request.params,
            format = request.format
        )

        val extension = request.format.lowercase()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.${extension}\"")
            .contentType(getMediaType(request.format))
            .body(bytes)
    }

    private fun getMediaType(format: String): MediaType {
        return when (format.uppercase()) {
            "PDF" -> MediaType.APPLICATION_PDF
            "XLSX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            "DOCX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
    }
}