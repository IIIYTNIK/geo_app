package com.example.geoserver.service

import com.stimulsoft.report.StiReport
import com.stimulsoft.report.StiSerializeManager
import com.stimulsoft.report.StiExportManager
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter 
import com.stimulsoft.report.export.settings.StiPdfExportSettings

@Service
class ReportService {

    init {
        // Настройка Stimulsoft для работы с отсутствующими шрифтами
        System.setProperty("stimulsoft.reports.fonts.check", "false")
        System.setProperty("stimulsoft.reports.pdf.embeddedFonts", "false")
        System.setProperty("stimulsoft.reports.fonts.default", "Times New Roman")
    }

    // Создаем форматтер для отображения (ДД.ММ.ГГГГ)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun generateDrillingCompletedReportPdf(
        reportStart: LocalDate?,
        reportEnd: LocalDate?,
        contractorId: Long = 0L,
        areaId: Long = 0L
    ): ByteArray {
        val report = loadReport()
        
        // Передаем даты уже как отформатированные строки
        report.dictionary.variables["ReportStart"].value = reportStart?.format(dateFormatter) ?: ""
        report.dictionary.variables["ReportEnd"].value = reportEnd?.format(dateFormatter) ?: ""
        report.dictionary.variables["contractorId"].value = contractorId.toString()
        report.dictionary.variables["areaId"].value = areaId.toString()
        
        report.render()
        println("Render complete, exporting to PDF...")
        val outputStream = ByteArrayOutputStream()

        // Создаем настройки экспорта
        val settings = StiPdfExportSettings()
        settings.setEmbeddedFonts(false) // Отключаем принудительное внедрение шрифтов

        // Передаем настройки в метод экспорта
        StiExportManager.exportPdf(report, settings, outputStream)

        println("Export complete, size=${outputStream.size()}")
        return outputStream.toByteArray()
    }

    fun generateDrillingCompletedReport(
        reportStart: LocalDate?,
        reportEnd: LocalDate?,
        contractorId: Long = 0L,
        areaId: Long = 0L
    ): ByteArray {
        val report = loadReport()

        report.dictionary.variables["ReportStart"].value = reportStart?.format(dateFormatter) ?: ""
        report.dictionary.variables["ReportEnd"].value = reportEnd?.format(dateFormatter) ?: ""
        report.dictionary.variables["contractorId"].value = contractorId.toString()
        report.dictionary.variables["areaId"].value = areaId.toString()

        report.render()
        val outputStream = ByteArrayOutputStream()
        StiExportManager.exportExcel(report, outputStream)
        return outputStream.toByteArray()
    }

    private fun loadReport(): StiReport {
        val resource = ClassPathResource("reports/drilling_report_times.mrt")
        return StiSerializeManager.deserializeReport(resource.inputStream)
    }
}