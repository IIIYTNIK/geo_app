package com.example.geoserver.report.engine

import com.example.geoserver.report.model.*

class ReportEngine(
    private val dataProvider: ReportDataProvider
) {

    fun generate(config: ReportConfig): ReportResult {
        val data = dataProvider.fetchData(config.filters)

        val sections = config.sections.map { section ->
            buildSection(section, data)
        }

        return ReportResult(
            title = config.title,
            sections = sections
        )
    }

    private fun buildSection(
        section: ReportSection,
        data: List<DataRow>
    ): TableSection {

        val columns = section.columns.map { it.title }

        val rows = data.map { row ->
            section.columns.map { column ->
                row[column.field].toString()
            }
        }

        return TableSection(
            title = section.title,
            columns = columns,
            rows = rows
        )
    }
}