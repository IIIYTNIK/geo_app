package com.example.geoapp.api.report

import java.time.LocalDate

data class ReportRequest(
    val reportType: String,
    val reportStart: String?,
    val reportEnd: String?,
    val contractorId: Long,
    val areaId: Long
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

data class ReportDataDto(
    val rows: List<ReportRowDto>,
    val metadata: ReportMetadata = ReportMetadata()
)