package com.example.geoserver.service

import org.springframework.stereotype.Service

@Service
class ReportService {

    fun getWorkings(): List<Map<String, Any>> {
        // пока заглушка
        return listOf(
            mapOf(
                "borehole_name" to "BH-1",
                "h_value" to 120.5,
                "x_coord" to 10.0,
                "y_coord" to 20.0
            ),
            mapOf(
                "borehole_name" to "BH-2",
                "h_value" to 95.3,
                "x_coord" to 15.0,
                "y_coord" to 25.0
            )
        )
    }
}