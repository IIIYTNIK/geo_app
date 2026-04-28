package com.example.geoserver.service

import com.example.geoserver.repository.ReportTemplateRepository
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import javax.sql.DataSource
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@Service
class UniversalReportService(
    private val dataSource: DataSource,
    private val repository: ReportTemplateRepository
) {

    fun generateGenericReport(
        templateId: Long,
        params: Map<String, Any?>,
        format: String
    ): ByteArray {

        val template = repository.findById(templateId)
            .orElseThrow { RuntimeException("Шаблон не найден") }

        val jasperReport = JasperCompileManager.compileReport(
            template.jrxmlContent.byteInputStream()
        )

        val fixedParams = params.mapValues { (_, value) ->
            when (value) {
                is Int -> value.toLong()
                is LocalDate -> Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant())
                is String -> {
                    try {
                        // Сначала пробуем стандартный ISO-формат (yyyy-MM-dd), который шлет DatePicker
                        val parsedDate = LocalDate.parse(value)
                        Date.from(parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    } catch (e: Exception) {
                        try {
                            // Если не вышло, пробуем русский формат (dd.MM.yyyy)
                            SimpleDateFormat("dd.MM.yyyy").parse(value)
                        } catch (e2: Exception) {
                            value 
                        }
                    }
                }
                else -> value
            }
        }

        dataSource.connection.use { connection ->
            val jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                fixedParams,
                connection
            )

            val outputStream = ByteArrayOutputStream()

            when (format.uppercase()) {
                "PDF" -> JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream)

                "XLSX" -> {
                    val exporter = JRXlsxExporter()
                    exporter.setExporterInput(SimpleExporterInput(jasperPrint))
                    exporter.setExporterOutput(SimpleOutputStreamExporterOutput(outputStream))
                    exporter.exportReport()
                }

                "DOCX" -> {
                    val exporter = JRDocxExporter()
                    exporter.setExporterInput(SimpleExporterInput(jasperPrint))
                    exporter.setExporterOutput(SimpleOutputStreamExporterOutput(outputStream))
                    exporter.exportReport()
                }

                else -> throw RuntimeException("Неподдерживаемый формат: $format")
            }

            return outputStream.toByteArray()
        }
    }
}