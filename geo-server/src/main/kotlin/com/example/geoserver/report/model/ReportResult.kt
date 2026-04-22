package com.example.geoserver.report.model

data class ReportResult(
    val title: String,
    val sections: List<TableSection>
)

data class TableSection(
    val title: String,
    val columns: List<String>,
    val rows: List<List<String>>
)