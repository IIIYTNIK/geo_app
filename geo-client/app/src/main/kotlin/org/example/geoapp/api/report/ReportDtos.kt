package com.example.geoapp.api.report

data class ReportGenerateRequest(
    val templateId: Long,
    val format: String = "PDF",
    val params: Map<String, Any?> = emptyMap()
)

data class ReportTemplateSummaryDto(
    val id: Long,
    val name: String,
    val description: String? = null
)

data class ReportTemplateDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val jrxmlContent: String,
    val metadata: ReportTemplateMetadataDto = ReportTemplateMetadataDto()
)

data class ReportTemplateMetadataDto(
    val parameters: List<ReportParameterDto> = emptyList()
)

data class ReportParameterDto(
    val name: String,
    val label: String,
    val type: ReportParameterType,
    val required: Boolean = false,
    val valueType: ReportValueType = ReportValueType.STRING,
    val options: List<ReportOptionDto> = emptyList()
)

data class ReportOptionDto(
    val value: String,
    val label: String
)

enum class ReportParameterType {
    DATE,
    TEXT,
    NUMBER,
    SELECT,
    BOOLEAN
}

enum class ReportValueType {
    STRING,
    NUMBER,
    DECIMAL,
    BOOLEAN
}