package com.example.geoserver.dto.report

/**
 * Запрос на генерацию отчёта.
 */
data class ReportGenerateRequest(
    val templateId: Long,
    val format: String = "PDF",
    val params: Map<String, Any?> = emptyMap()
)

/**
 * Краткая информация о шаблоне отчёта.
 */
data class ReportTemplateSummaryDto(
    val id: Long,
    val name: String,
    val description: String? = null
)

/**
 * Полная информация о шаблоне отчёта.
 */
data class ReportTemplateDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val jrxmlContent: String,
    val metadata: ReportTemplateMetadataDto = ReportTemplateMetadataDto()
)

/**
 * Метаданные шаблона отчёта, которые клиент будет использовать для построения формы.
 */
data class ReportTemplateMetadataDto(
    val parameters: List<ReportParameterDto> = emptyList()
)

/**
 * Параметр отчёта.
 */
data class ReportParameterDto(
    val name: String,
    val label: String,
    val type: ReportParameterType,
    val required: Boolean = false,
    val valueType: ReportValueType = ReportValueType.STRING,
    val options: List<ReportOptionDto> = emptyList()
)

/**
 * Возможные варианты для SELECT-параметров.
 */
data class ReportOptionDto(
    val value: String,
    val label: String
)

/**
 * Тип параметра отчёта.
 */
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