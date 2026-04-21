package com.example.geoserver.service

import net.sf.dynamicreports.report.builder.DynamicReports
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.exception.DRException
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ReportService {

    data class ReportRow(
        val rowNumber: Int,
        val wellName: String,
        val depth: Double
    )

    fun generateDrillingCompletedReportPdf(
        reportStart: LocalDate?,
        reportEnd: LocalDate?,
        contractorId: Long = 0L,
        areaId: Long = 0L
    ): ByteArray {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val startStr = reportStart?.format(dateFormatter) ?: "не указано"
        val endStr = reportEnd?.format(dateFormatter) ?: "не указано"

        val title = buildString {
            append("Отчёт о выполненном бурении")
            if (contractorId != 0L) append(" (Подрядчик ID: $contractorId)")
            if (areaId != 0L) append(" (Участок ID: $areaId)")
            append("\nПериод: $startStr – $endStr")
        }

        // Тестовые данные (замените на реальные из БД позже)
        val testData = listOf(
            ReportRow(1, "Скважина 1", 120.5),
            ReportRow(2, "Скважина 2", 85.0),
            ReportRow(3, "Скважина 3", 150.0)
        )
        val dataSource = JRBeanCollectionDataSource(testData)

        // Стиль заголовков колонок с поддержкой кириллицы (шрифт Arial)
        val columnTitleStyle = DynamicReports.stl.style()
            .setFontName("Arial")
            .setBold(true)
            .setFontSize(12)
            .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)

        // Стиль для обычных ячеек таблицы
        val cellStyle = DynamicReports.stl.style()
            .setFontName("Arial")
            .setFontSize(10)

        // Стиль для заголовка отчёта
        val titleStyle = DynamicReports.stl.style()
            .setFontName("Arial")
            .setBold(true)
            .setFontSize(14)

        // Колонки (явное указание типов дженериков помогает компилятору)
        val rowNumCol: TextColumnBuilder<Int> = DynamicReports.col.column(
            "№ п/п", "rowNumber", DynamicReports.type.integerType()
        ).setStyle(cellStyle)

        val wellNameCol: TextColumnBuilder<String> = DynamicReports.col.column(
            "Наименование скважины", "wellName", DynamicReports.type.stringType()
        ).setStyle(cellStyle)

        val depthCol: TextColumnBuilder<Double> = DynamicReports.col.column(
            "Глубина, м", "depth", DynamicReports.type.doubleType()
        ).setStyle(cellStyle)

        try {
            val report = DynamicReports.report()
                .setColumnTitleStyle(columnTitleStyle)
                .columns(rowNumCol, wellNameCol, depthCol)
                .title(
                    DynamicReports.cmp.text(title).setStyle(titleStyle)
                )
                .pageFooter(
                    DynamicReports.cmp.pageNumber()
                        .setStyle(DynamicReports.stl.style().setFontName("Arial"))
                )
                .setDataSource(dataSource)

            val outputStream = ByteArrayOutputStream()
            report.toPdf(outputStream)
            return outputStream.toByteArray()
        } catch (e: DRException) {
            throw RuntimeException("Ошибка генерации PDF отчёта", e)
        }
    }
}