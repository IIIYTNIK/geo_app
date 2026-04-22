package com.example.geoserver.report.model

import java.time.LocalDate

data class ReportConfig(
    val title: String,
    val filters: Filters,
    val sections: List<ReportSection>
)

data class Filters(
    val boreholeIds: List<Long>? = null,
    val areaIds: List<Long>? = null,
    val geologistIds: List<Long>? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null
)

data class ReportSection(
    val title: String,
    val columns: List<ColumnConfig>
)

data class ColumnConfig(
    val field: String,   // имя поля из DTO
    val title: String    // отображаемое имя
)