package com.example.geoserver.dto

import java.time.LocalDate

data class ReportDataDto(
    val rows: List<Map<String, Any?>>,
    val metadata: ReportMetadata = ReportMetadata()
)

data class ReportRowDto(
    val boreholeName: String?,
    val hValue: Double?,
    val xCoord: Double?,
    val yCoord: Double?,
    val zCoord: Double?,
    val startDate: String?,
    val endDate: String?,
    val geologistName: String?
)

data class ReportMetadata(
    val totalRows: Int = 0,
    val summary: Map<String, Any?> = emptyMap(),
    val appliedFilters: Map<String, Any?> = emptyMap(),
    val reportDate: LocalDate = LocalDate.now()
)