package org.example.geoapp.report.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.example.geoapp.report.model.ReportResult
import org.example.geoapp.report.model.ReportTable
import java.io.File
import java.io.OutputStream

/**
 * Экспортирует ReportResult в PDF через PDFBox.
 * Поддержка кириллицы, аккуратное оформление, перенос страниц.
 */
object ReportPdfExporter {
    /**
     * Экспортирует отчёт в PDF-файл.
     * @param result результат отчёта
     * @param file файл для сохранения
     * @param fontFile файл шрифта с поддержкой кириллицы (например, resources/fonts/Roboto-Regular.ttf)
     */
    fun exportToPdf(result: ReportResult, file: File, fontFile: File) {
        PDDocument().use { doc ->
            val font = PDType0Font.load(doc, fontFile)
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            var content = PDPageContentStream(doc, page)
            val margin = 40f
            var y = page.mediaBox.height - margin
            val lineHeight = 18f
            val fontSize = 12f

            // Заголовок
            content.beginText()
            content.setFont(font, 16f)
            content.newLineAtOffset(margin, y)
            content.showText(result.config.title)
            content.endText()
            y -= lineHeight * 1.5f

            // Фильтры (если есть)
            if (result.config.filters.isNotEmpty()) {
                content.beginText()
                content.setFont(font, fontSize)
                content.newLineAtOffset(margin, y)
                content.showText("Параметры: " + result.config.filters.joinToString { it.label })
                content.endText()
                y -= lineHeight
            }

            // Таблицы
            for (table in result.tables) {
                // Заголовок секции
                content.beginText()
                content.setFont(font, 14f)
                content.newLineAtOffset(margin, y)
                content.showText(table.section.sectionTitle)
                content.endText()
                y -= lineHeight

                // Заголовки колонок
                content.beginText()
                content.setFont(font, fontSize)
                content.newLineAtOffset(margin, y)
                val headers = table.section.columns.joinToString(" | ") { it.header }
                content.showText(headers)
                content.endText()
                y -= lineHeight

                // Строки
                for (row in table.rows) {
                    val rowText = table.section.columns.joinToString(" | ") { col ->
                        val value = row.values[col.field]
                        value?.toString() ?: ""
                    }
                    if (y < margin + lineHeight * 2) {
                        content.close()
                        val newPage = PDPage(PDRectangle.A4)
                        doc.addPage(newPage)
                        y = newPage.mediaBox.height - margin
                        content = PDPageContentStream(doc, newPage)
                    }
                    content.beginText()
                    content.setFont(font, fontSize)
                    content.newLineAtOffset(margin, y)
                    content.showText(rowText)
                    content.endText()
                    y -= lineHeight
                }
                y -= lineHeight * 0.5f
            }
            content.close()
            doc.save(file)
        }
    }
}
