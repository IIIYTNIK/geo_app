package org.example.geoapp.controller

import com.example.geoapp.api.report.*
import com.example.geoapp.api.RefArea
import com.example.geoapp.api.RefContractor
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.FileChooser
import javafx.util.StringConverter
import org.example.geoapp.MainApp
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.await
import java.time.LocalDate
import okhttp3.ResponseBody
import kotlinx.coroutines.*

import org.example.geoapp.report.engine.ReportEngine
import org.example.geoapp.report.model.*
import org.example.geoapp.report.ui.DynamicReportTable
import javafx.scene.layout.VBox

class ReportDialogController {
        @FXML private lateinit var btnExportPdf: Button
        private lateinit var token: String

        @FXML private lateinit var comboReportType: ComboBox<String>
        @FXML private lateinit var comboArea: ComboBox<RefArea>
        @FXML private lateinit var comboContractor: ComboBox<RefContractor>
        @FXML private lateinit var dateStart: DatePicker
        @FXML private lateinit var dateEnd: DatePicker
        @FXML private lateinit var comboFormat: ComboBox<String>
        @FXML private lateinit var vboxReportTables: VBox
        /**
         * Экспорт текущего отчёта в PDF через PDFBox.
         */
        @FXML
        private fun onExportPdfClick() {
            // Проверяем, есть ли сгенерированный отчёт
            val filters = collectFilters()
            val config = ReportConfig(
                title = "Бурение на участке по подрядчику",
                filters = emptyList(),
                sections = listOf(
                    ReportSection(
                        sectionTitle = "Основная таблица",
                        columns = listOf(
                            ColumnConfig("boreholeName", "Скважина"),
                            ColumnConfig("hValue", "Глубина"),
                            ColumnConfig("xCoord", "X"),
                            ColumnConfig("yCoord", "Y"),
                            ColumnConfig("zCoord", "Z"),
                            ColumnConfig("startDate", "Начало"),
                            ColumnConfig("endDate", "Окончание"),
                            ColumnConfig("geologistName", "Геолог")
                        )
                    )
                )
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = reportEngine.generate(config, filters, "Bearer $token")
                    withContext(Dispatchers.Main) {
                        val fileChooser = FileChooser()
                        fileChooser.title = "Сохранить PDF отчёт"
                        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PDF", "*.pdf"))
                        val file = fileChooser.showSaveDialog(vboxReportTables.scene.window)
                        if (file != null) {
                            // Путь к Arial.ttf в ресурсах клиента
                            val fontStream = javaClass.getResourceAsStream("/fonts/Arial.ttf")
                            if (fontStream == null) {
                                Alert(Alert.AlertType.ERROR, "Файл шрифта Arial.ttf не найден в resources/fonts").showAndWait()
                                return@withContext
                            }
                            val tempFont = kotlin.io.path.createTempFile(suffix = ".ttf").toFile()
                            fontStream.use { input -> tempFont.outputStream().use { output -> input.copyTo(output) } }
                            try {
                                org.example.geoapp.report.pdf.ReportPdfExporter.exportToPdf(result, file, tempFont)
                                Alert(Alert.AlertType.INFORMATION, "PDF успешно сохранён: ${file.absolutePath}").showAndWait()
                            } finally {
                                tempFont.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Alert(Alert.AlertType.ERROR, "Ошибка экспорта PDF: ${e.message}").showAndWait()
                    }
                }
            }
        }


    /**
     * Формирует фильтры для запроса отчёта, строго соблюдая формат дат и работу с null.
     */
    // Храним текущую конфигурацию фильтров для универсальной сборки
    private var currentFilterConfig: List<FilterConfig> = emptyList()

    /**
     * Универсальный сборщик фильтров по FilterConfig из ReportConfig
     */
    private fun collectFilters(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for (filter in currentFilterConfig) {
            when (filter.field) {
                "reportType" -> result["reportType"] = comboReportType.value
                "areaId" -> {
                    val id = comboArea.value?.id ?: 0L
                    result["areaId"] = if (id == 0L) null else id
                }
                "contractorId" -> {
                    val id = comboContractor.value?.id ?: 0L
                    result["contractorId"] = if (id == 0L) null else id
                }
                "reportStart" -> {
                    val start = dateStart.value
                    result["reportStart"] = start?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(it) }
                }
                "reportEnd" -> {
                    val end = dateEnd.value
                    result["reportEnd"] = end?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(it) }
                }
                else -> {
                    // Для будущих фильтров типа SELECT/TEXT
                    // result[filter.field] = ...
                }
            }
        }
        return result
    }

    private val reportEngine by lazy { ReportEngine(MainApp.api) }

    fun initData(token: String, areas: List<RefArea>, contractors: List<RefContractor>) {
        this.token = token
        // Конфиг фильтров для текущего отчёта (можно расширять)
        currentFilterConfig = listOf(
            FilterConfig("reportType", "Тип отчёта", FilterType.SELECT, options = listOf(
                FilterOption("drilling_completed", "Бурение на участке по подрядчику")
            )),
            FilterConfig("areaId", "Участок", FilterType.SELECT, options = (listOf(FilterOption(null, "--- Все участки ---")) + areas.map { FilterOption(it.id, it.name) })),
            FilterConfig("contractorId", "Подрядчик", FilterType.SELECT, options = (listOf(FilterOption(null, "--- Все подрядчики ---")) + contractors.map { FilterOption(it.id, it.name) })),
            FilterConfig("reportStart", "Дата с", FilterType.DATE),
            FilterConfig("reportEnd", "Дата по", FilterType.DATE)
        )

        // UI инициализация по фильтрам
        comboReportType.items = FXCollections.observableArrayList(currentFilterConfig.first { it.field == "reportType" }.options!!.map { it.label })
        comboReportType.selectionModel.selectFirst()

        comboFormat.items = FXCollections.observableArrayList("PDF")
        comboFormat.selectionModel.selectFirst()

        val allAreas = (currentFilterConfig.first { it.field == "areaId" }.options!!.mapIndexed { idx, opt ->
            if (idx == 0) RefArea(0, opt.label) else areas[idx - 1]
        })
        val allContractors = (currentFilterConfig.first { it.field == "contractorId" }.options!!.mapIndexed { idx, opt ->
            if (idx == 0) RefContractor(0, opt.label) else contractors[idx - 1]
        })

        comboArea.items = FXCollections.observableArrayList(allAreas)
        comboContractor.items = FXCollections.observableArrayList(allContractors)
        comboArea.setupNameConverter { it.name }
        comboContractor.setupNameConverter { it.name }
        comboArea.selectionModel.selectFirst()
        comboContractor.selectionModel.selectFirst()

        dateStart.value = LocalDate.now().minusMonths(1)
        dateEnd.value = LocalDate.now()
    }

    @FXML
    private fun onGenerateClick() {
        val start = dateStart.value
        val end = dateEnd.value

        // Проверка дат
        if (start != null && end != null && start.isAfter(end)) {
            Alert(Alert.AlertType.ERROR, "Дата начала не может быть позже даты окончания").showAndWait()
            return
        }

        val filters = collectFilters()

        // Пример конфигурации отчёта (можно вынести в отдельный метод/файл)
        val config = ReportConfig(
            title = "Бурение на участке по подрядчику",
            filters = emptyList(),
            sections = listOf(
                ReportSection(
                    sectionTitle = "Основная таблица",
                    columns = listOf(
                        ColumnConfig("boreholeName", "Скважина"),
                        ColumnConfig("hValue", "Глубина"),
                        ColumnConfig("xCoord", "X"),
                        ColumnConfig("yCoord", "Y"),
                        ColumnConfig("zCoord", "Z"),
                        ColumnConfig("startDate", "Начало"),
                        ColumnConfig("endDate", "Окончание"),
                        ColumnConfig("geologistName", "Геолог")
                    )
                ),
                ReportSection(
                    sectionTitle = "Сводка по подрядчикам",
                    columns = listOf(
                        ColumnConfig("geologistName", "Геолог"),
                        ColumnConfig("boreholeName", "Скважин"),
                        ColumnConfig("hValue", "Суммарная глубина")
                    )
                )
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = reportEngine.generate(config, filters, "Bearer $token")
                withContext(Dispatchers.Main) {
                    vboxReportTables.children.clear()
                    if (result.tables.isEmpty() || result.tables.all { it.rows.isEmpty() }) {
                        vboxReportTables.children.add(Label("Нет данных для отображения"))
                    } else {
                        for (table in result.tables) {
                            val tableView = DynamicReportTable.buildTable(table)
                            vboxReportTables.children.add(Label(table.section.sectionTitle))
                            vboxReportTables.children.add(tableView)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Alert(Alert.AlertType.ERROR, "Ошибка: ${e.message}").showAndWait()
                }
            }
        }
    }

    private fun <T> ComboBox<T>.setupNameConverter(extractor: (T) -> String) {
        this.converter = object : StringConverter<T>() {
            override fun toString(obj: T?): String = obj?.let(extractor) ?: ""
            override fun fromString(string: String?): T? {
                return items.find { extractor(it) == string }
            }
        }
    }
}