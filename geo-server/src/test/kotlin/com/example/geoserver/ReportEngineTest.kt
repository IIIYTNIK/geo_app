package com.example.geoserver

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.example.geoserver.report.engine.*
import com.example.geoserver.report.model.*

class ReportEngineTest {

    @Test
    @DisplayName("should build report correctly")
    fun shouldBuildReportCorrectly() {

        val provider = object : ReportDataProvider {
            override fun fetchData(filters: Filters): List<DataRow> {
                return listOf(
                    mapOf("name" to "BH-1", "depth" to 100),
                    mapOf("name" to "BH-2", "depth" to 200)
                )
            }
        }

        val engine = ReportEngine(provider)

        val config = ReportConfig(
            title = "Test Report",
            filters = Filters(),
            sections = listOf(
                ReportSection(
                    title = "Section 1",
                    columns = listOf(
                        ColumnConfig("name", "Name"),
                        ColumnConfig("depth", "Depth")
                    )
                )
            )
        )

        val result = engine.generate(config)

        assertEquals(1, result.sections.size)
        assertEquals(2, result.sections[0].rows.size)
        assertEquals("BH-1", result.sections[0].rows[0][0])
    }
}