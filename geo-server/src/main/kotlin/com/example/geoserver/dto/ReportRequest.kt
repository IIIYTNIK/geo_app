package com.example.geoserver.dto

import java.time.LocalDate

/**
 * Параметры, которые клиент передаёт на сервер для формирования данных отчёта.
 */
data class ReportRequest(
    val reportType: String,                    // Например: "drilling_completed", "productivity", "planned" и т.д.
    val reportStart: String? = null,           // yyyy-MM-dd
    val reportEnd: String? = null,             // yyyy-MM-dd
    val contractorId: Long? = null,
    val areaId: Long? = null,
    // val geologistId: Long? = null,
    // val drillingRigId: Long? = null,
    // Можно добавлять другие фильтры по мере необходимости
)