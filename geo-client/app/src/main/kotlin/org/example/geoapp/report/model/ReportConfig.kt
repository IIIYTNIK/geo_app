package org.example.geoapp.report.model

/**
 * Конфигурация отчёта: описывает структуру, фильтры, секции, колонки, порядок, заголовки.
 */
data class ReportConfig(
    val title: String,
    val filters: List<FilterConfig> = emptyList(),
    val sections: List<ReportSection> = emptyList()
)

/**
 * Описание секции (таблицы) в отчёте.
 */
data class ReportSection(
    val sectionTitle: String,
    val columns: List<ColumnConfig>,
    val type: SectionType = SectionType.TABLE
)

enum class SectionType {
    TABLE,
    // В будущем: CHART, SUMMARY и др.
}

/**
 * Описание колонки таблицы.
 */
data class ColumnConfig(
    val field: String,         // Имя поля в DTO
    val header: String,        // Заголовок колонки
    val width: Double? = null, // Ширина колонки (опционально)
    val format: String? = null // Формат отображения (опционально)
)

/**
 * Описание фильтра для отчёта.
 */
data class FilterConfig(
    val field: String,         // Имя поля фильтра
    val label: String,         // Заголовок для UI
    val type: FilterType,      // Тип фильтра (DATE, SELECT, TEXT)
    val options: List<FilterOption>? = null // Для select
)

enum class FilterType {
    DATE, SELECT, TEXT
}

data class FilterOption(
    val value: Any?,
    val label: String
)

/**
 * Результат генерации отчёта (для передачи между слоями)
 */
data class ReportResult(
    val config: ReportConfig,
    val tables: List<ReportTable>
)

data class ReportTable(
    val section: ReportSection,
    val rows: List<ReportRow>
)

/**
 * Универсальная строка таблицы (ключ-значение)
 */
data class ReportRow(
    val values: Map<String, Any?>
)
