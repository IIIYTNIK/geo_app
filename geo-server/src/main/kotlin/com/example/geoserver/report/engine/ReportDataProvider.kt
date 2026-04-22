package com.example.geoserver.report.engine

import com.example.geoserver.report.model.DataRow
import com.example.geoserver.report.model.Filters

interface ReportDataProvider {
    fun fetchData(filters: Filters): List<DataRow>
}