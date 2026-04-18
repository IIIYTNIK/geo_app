package com.example.geoserver.controller

import com.example.geoserver.service.ReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@RestController
@RequestMapping("/api/reports")
class ReportController(private val reportService: ReportService) {

    @PostMapping("/drilling-completed/pdf")
    fun generateDrillingCompletedReportPdf(
        @RequestParam reportStart: LocalDate?,
        @RequestParam reportEnd: LocalDate?,
        @RequestParam(defaultValue = "0") contractorId: Long,
        @RequestParam(defaultValue = "0") areaId: Long
    ): ResponseEntity<ByteArray> {
        println("=== generateDrillingCompletedReportPdf called ===")
        println("reportStart=$reportStart, reportEnd=$reportEnd, contractorId=$contractorId, areaId=$areaId")
        val bytes = reportService.generateDrillingCompletedReportPdf(
            reportStart, reportEnd, contractorId, areaId
        )
        val filename = "Отчет_Бурение_Выполненный.pdf"
        val encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes)
    }
}