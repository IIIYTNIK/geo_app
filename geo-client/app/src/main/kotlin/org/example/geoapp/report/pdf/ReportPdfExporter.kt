package org.example.geoapp.report.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.example.geoapp.report.model.ReportResult
import org.example.geoapp.report.model.ReportTable
import org.example.geoapp.report.model.ColumnConfig
import java.io.File

/**
 * Экспортирует ReportResult в PDF через PDFBox.
 * Поддерживает таблицы с границами, многостраничность и кириллицу.
 */
object ReportPdfExporter {
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 40f
    private const val FONT_SIZE_TITLE = 16f
    private const val FONT_SIZE_SECTION = 14f
    private const val FONT_SIZE_NORMAL = 11f
    private const val ROW_HEIGHT = 24f
    private const val CELL_PADDING = 4f
    private const val BORDER_WIDTH = 0.5f
    private const val MIN_COLUMN_WIDTH = 50f

    /**
     * Экспортирует отчёт в PDF-файл.
     * @param result результат отчёта
     * @param file файл для сохранения
     * @param fontFile файл шрифта с поддержкой кириллицы (например, Arial.ttf)
     */
    fun exportToPdf(result: ReportResult, file: File, fontFile: File) {
        PDDocument().use { doc ->
            val font = PDType0Font.load(doc, fontFile)
            
            val pageWidth = PDRectangle.A4.width - MARGIN_LEFT - MARGIN_RIGHT
            
            // Создаём первую страницу и стартуем
            var currentPage = PDPage(PDRectangle.A4)
            doc.addPage(currentPage)
            var contentStream = PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.OVERWRITE, false)
            var currentY = currentPage.mediaBox.height - MARGIN_TOP
            
            try {
                // Выводим заголовок отчёта
                currentY = drawText(contentStream, font, FONT_SIZE_TITLE, result.config.title, MARGIN_LEFT, currentY)
                currentY -= 12f
                
                // Выводим параметры фильтров (если есть)
                if (result.config.filters.isNotEmpty()) {
                    val filterText = "Параметры: " + result.config.filters.joinToString(", ") { it.label }
                    currentY = drawText(contentStream, font, FONT_SIZE_NORMAL, filterText, MARGIN_LEFT, currentY)
                    currentY -= 12f
                }
                
                currentY -= 8f // Пробел перед таблицами
                
                // Выводим каждую таблицу
                for (table in result.tables) {
                    // Заголовок секции
                    currentY = drawText(contentStream, font, FONT_SIZE_SECTION, table.section.sectionTitle, MARGIN_LEFT, currentY)
                    currentY -= 12f
                    
                    // Вычисляем ширину колонок на основе их количества и максимального содержимого
                    val columnWidths = calculateColumnWidths(table, font, FONT_SIZE_NORMAL, pageWidth)
                    
                    // Проверяем, хватит ли места на текущей странице для заголовков + хотя бы одной строки
                    val tableHeaderHeight = ROW_HEIGHT + 2f
                    if (currentY - tableHeaderHeight - ROW_HEIGHT < MARGIN_BOTTOM) {
                        // Переходим на новую страницу
                        contentStream.close()
                        currentPage = PDPage(PDRectangle.A4)
                        doc.addPage(currentPage)
                        contentStream = PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.OVERWRITE, false)
                        currentY = currentPage.mediaBox.height - MARGIN_TOP
                        
                        // Повторяем заголовок секции на новой странице
                        currentY = drawText(contentStream, font, FONT_SIZE_SECTION, table.section.sectionTitle, MARGIN_LEFT, currentY)
                        currentY -= 12f
                    }
                    
                    // Рисуем заголовок таблицы (с границами)
                    currentY = drawTableHeader(contentStream, font, FONT_SIZE_NORMAL, 
                        table.section.columns, columnWidths, MARGIN_LEFT, currentY)
                    
                    // Рисуем строки данных
                    for (row in table.rows) {
                        // Проверяем, хватит ли места для одной строки
                        if (currentY - ROW_HEIGHT < MARGIN_BOTTOM) {
                            // Переходим на новую страницу
                            contentStream.close()
                            currentPage = PDPage(PDRectangle.A4)
                            doc.addPage(currentPage)
                            contentStream = PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.OVERWRITE, false)
                            currentY = currentPage.mediaBox.height - MARGIN_TOP
                            
                            // Повторяем заголовок секции и таблицы на новой странице
                            currentY = drawText(contentStream, font, FONT_SIZE_SECTION, table.section.sectionTitle, MARGIN_LEFT, currentY)
                            currentY -= 12f
                            currentY = drawTableHeader(contentStream, font, FONT_SIZE_NORMAL, 
                                table.section.columns, columnWidths, MARGIN_LEFT, currentY)
                        }
                        
                        // Рисуем строку данных
                        val rowValues = table.section.columns.map { col ->
                            row.values[col.field]?.toString() ?: ""
                        }
                        currentY = drawTableRow(contentStream, font, FONT_SIZE_NORMAL, 
                            rowValues, columnWidths, MARGIN_LEFT, currentY)
                    }
                    
                    currentY -= 16f // Пробел между таблицами
                }
            } finally {
                contentStream.close()
            }
            
            doc.save(file)
        }
    }
    
    /**
     * Вычисляет ширину каждой колонки на основе содержимого и доступного пространства.
     */
    private fun calculateColumnWidths(table: ReportTable, font: PDType0Font, fontSize: Float, availableWidth: Float): List<Float> {
        val columns = table.section.columns
        val colCount = columns.size
        
        // Базовая ширина для каждой колонки
        val baseWidth = (availableWidth - colCount * BORDER_WIDTH * 2) / colCount
        val columnWidths = mutableListOf<Float>()
        
        for (col in columns) {
            // Проверяем максимальную ширину требуемого контента
            var maxWidth = getStringWidth(font, fontSize, col.header) + CELL_PADDING * 2
            
            for (row in table.rows) {
                val value = row.values[col.field]?.toString() ?: ""
                val width = getStringWidth(font, fontSize, value) + CELL_PADDING * 2
                if (width > maxWidth) {
                    maxWidth = width
                }
            }
            
            // Если минимальная ширина больше базовой, используем минимальную
            columnWidths.add(kotlin.math.max(baseWidth, kotlin.math.min(maxWidth, baseWidth * 2)))
        }
        
        // Нормализуем ширины так, чтобы они вмещались в доступное пространство
        val totalWidth = columnWidths.sum()
        if (totalWidth > availableWidth) {
            val scale = availableWidth / totalWidth
            return columnWidths.map { it * scale }
        }
        
        return columnWidths
    }
    
    /**
     * Примерная ширина текста в пунктах.
     */
    private fun getStringWidth(font: PDType0Font, fontSize: Float, text: String): Float {
        // Это упрощённый расчёт; для точности нужна информация о шрифте
        // В среднем каждый символ занимает примерно fontSize * 0.6 пунктов
        return text.length * fontSize * 0.5f
    }
    
    /**
     * Рисует заголовок таблицы с границами.
     */
    private fun drawTableHeader(contentStream: PDPageContentStream, font: PDType0Font, fontSize: Float,
                               columns: List<ColumnConfig>, columnWidths: List<Float>, 
                               startX: Float, startY: Float): Float {
        var x = startX
        val headerHeight = ROW_HEIGHT
        
        // Рисуем ячейки заголовка с границами
        for ((idx, col) in columns.withIndex()) {
            val width = columnWidths[idx]
            
            // Рисуем границу ячейки
            drawRectangle(contentStream, x, startY - headerHeight, width, headerHeight)
            
            // Пишем текст заголовка (обрезаем если не помещается)
            val displayText = truncateText(font, fontSize, col.header, width - CELL_PADDING * 2)
            drawCenteredText(contentStream, font, fontSize, displayText, x + width / 2, startY - headerHeight / 2 - fontSize / 4)
            
            x += width
        }
        
        return startY - headerHeight
    }
    
    /**
     * Рисует строку таблицы с границами.
     */
    private fun drawTableRow(contentStream: PDPageContentStream, font: PDType0Font, fontSize: Float,
                            values: List<String>, columnWidths: List<Float>, 
                            startX: Float, startY: Float): Float {
        var x = startX
        val rowHeight = ROW_HEIGHT
        
        // Рисуем ячейки данных с границами
        for ((idx, value) in values.withIndex()) {
            val width = columnWidths[idx]
            
            // Рисуем границу ячейки
            drawRectangle(contentStream, x, startY - rowHeight, width, rowHeight)
            
            // Пишем текст (обрезаем если не помещается)
            val displayText = truncateText(font, fontSize, value, width - CELL_PADDING * 2)
            drawLeftAlignedText(contentStream, font, fontSize, displayText, x + CELL_PADDING, startY - rowHeight / 2 - fontSize / 4)
            
            x += width
        }
        
        return startY - rowHeight
    }
    
    /**
     * Рисует прямоугольник (границу ячейки).
     */
    private fun drawRectangle(contentStream: PDPageContentStream, x: Float, y: Float, width: Float, height: Float) {
        contentStream.setLineWidth(BORDER_WIDTH)
        contentStream.addRect(x, y, width, height)
        contentStream.stroke()
    }
    
    /**
     * Обрезает текст если он не помещается в заданную ширину.
     */
    private fun truncateText(font: PDType0Font, fontSize: Float, text: String, maxWidth: Float): String {
        if (getStringWidth(font, fontSize, text) <= maxWidth) {
            return text
        }
        
        var truncated = text
        while (truncated.isNotEmpty() && getStringWidth(font, fontSize, truncated + "...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return truncated + (if (text.length > truncated.length) "..." else "")
    }
    
    /**
     * Выводит выравненный слева текст.
     */
    private fun drawLeftAlignedText(contentStream: PDPageContentStream, font: PDType0Font, fontSize: Float,
                                   text: String, x: Float, y: Float) {
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x, y)
        contentStream.showText(text)
        contentStream.endText()
    }
    
    /**
     * Выводит выравненный по центру текст.
     */
    private fun drawCenteredText(contentStream: PDPageContentStream, font: PDType0Font, fontSize: Float,
                                text: String, x: Float, y: Float) {
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x - getStringWidth(font, fontSize, text) / 2, y)
        contentStream.showText(text)
        contentStream.endText()
    }
    
    /**
     * Выводит обычный текст и возвращает новое значение Y.
     */
    private fun drawText(contentStream: PDPageContentStream, font: PDType0Font, fontSize: Float,
                        text: String, x: Float, y: Float): Float {
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x, y - fontSize)
        contentStream.showText(text)
        contentStream.endText()
        return y - fontSize - 4f
    }
}
