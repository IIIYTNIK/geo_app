package com.example.geoserver.dto

data class ReportRequest(
    val templateId: Long,
    val format: String = "PDF",
    val params: Map<String, Any> = emptyMap()
)