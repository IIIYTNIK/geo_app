package org.example.geoapp.report.engine

import com.example.geoapp.api.GeoApi
import com.example.geoapp.api.report.ReportRequest
import com.example.geoapp.api.report.ReportDataDto
import org.example.geoapp.report.model.*
import retrofit2.Call
import retrofit2.Response

/**
 * ReportEngine — преобразует ReportConfig и данные с backend в итоговую структуру отчёта.
 * Не содержит UI-логики и не зависит от JavaFX.
 */
class ReportEngine(private val geoApi: GeoApi) {

    /**
     * Генерирует отчёт по конфигурации: запрашивает данные, собирает структуру для UI и PDF.
     */
    fun generate(config: ReportConfig, filters: Map<String, Any?>, token: String): ReportResult {
        // Формируем запрос к backend
        val request = buildReportRequest(config, filters)
        val call: Call<ReportDataDto> = geoApi.getReportData(token, request)
        val response: Response<ReportDataDto> = call.execute()
        if (!response.isSuccessful || response.body() == null) {
            throw RuntimeException("Ошибка получения данных отчёта: ${response.code()} ${response.message()}")
        }
        val data = response.body()!!
        // Преобразуем данные в универсальную структуру
        val tables = config.sections.map { section ->
            val rows = data.rows.map { rowDto ->
                val values = section.columns.associate { col ->
                    col.field to getFieldValue(rowDto, col.field)
                }
                ReportRow(values)
            }
            ReportTable(section, rows)
        }
        return ReportResult(config, tables)
    }

    /**
     * Формирует ReportRequest для backend на основе фильтров и конфигурации.
     */
    private fun buildReportRequest(config: ReportConfig, filters: Map<String, Any?>): ReportRequest {
        // Пример для стандартных фильтров, можно расширять при необходимости
        return ReportRequest(
            reportType = filters["reportType"] as? String ?: error("reportType обязателен"),
            reportStart = filters["reportStart"] as? String,
            reportEnd = filters["reportEnd"] as? String,
            contractorId = filters["contractorId"] as? Long?,
            areaId = filters["areaId"] as? Long?
        )
    }

    /**
     * Универсальный getter для DTO (через reflection)
     */
    private fun getFieldValue(dto: Any, field: String): Any? {
        return try {
            val prop = dto::class.members.firstOrNull { it.name == field } ?: return null
            prop.call(dto)
        } catch (e: Exception) {
            null
        }
    }
}
