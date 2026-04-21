package org.example.geoapp.controller

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

class ReportDialogController {
    private lateinit var token: String

    @FXML private lateinit var comboReportType: ComboBox<String>
    @FXML private lateinit var comboArea: ComboBox<RefArea>
    @FXML private lateinit var comboContractor: ComboBox<RefContractor>
    @FXML private lateinit var dateStart: DatePicker
    @FXML private lateinit var dateEnd: DatePicker
    @FXML private lateinit var comboFormat: ComboBox<String>

    fun initData(token: String, areas: List<RefArea>, contractors: List<RefContractor>) {
        this.token = token

        comboReportType.items = FXCollections.observableArrayList("Бурение на участке по подрядчику")
        comboReportType.selectionModel.selectFirst()

        comboFormat.items = FXCollections.observableArrayList("PDF")
        comboFormat.selectionModel.selectFirst()

        val allAreas = listOf(RefArea(0, "--- Все участки ---")) + areas
        val allContractors = listOf(RefContractor(0, "--- Все подрядчики ---")) + contractors

        comboArea.items = FXCollections.observableArrayList(allAreas)
        comboContractor.items = FXCollections.observableArrayList(allContractors)
        
        // Используем вашу же функцию setupNameConverter для чистоты кода
        comboArea.setupNameConverter { it.name }
        comboContractor.setupNameConverter { it.name }

        comboArea.selectionModel.selectFirst()
        comboContractor.selectionModel.selectFirst()
        
        dateStart.value = LocalDate.now().minusMonths(1)
        dateEnd.value = LocalDate.now()
    }

    @FXML
    private fun onGenerateClick() {
        val areaId = comboArea.value?.id ?: 0
        val contractorId = comboContractor.value?.id ?: 0
        val start = dateStart.value ?: LocalDate.of(2000, 1, 1)
        val end = dateEnd.value ?: LocalDate.of(2100, 1, 1)
        val format = comboFormat.value

        val startStr = start.toString()
        val endStr = end.toString()
        
        println("=== Generate Report Clicked ===")
        
        runOnFx {
            try {
                // Вызываем API и получаем ResponseBody
                val response: ResponseBody = if (format == "PDF") {
                    MainApp.api.generateReportPdf("Bearer $token", startStr, endStr, contractorId, areaId).await()
                } else {
                    MainApp.api.generateReportExcel("Bearer $token", startStr, endStr, contractorId, areaId).await()
                }

                val bytes = response.bytes()

                val extension = if (format == "PDF") "pdf" else "xlsx"
                val fileChooser = FileChooser().apply {
                    title = "Сохранить отчёт"
                    extensionFilters.add(FileChooser.ExtensionFilter(
                        if (format == "PDF") "PDF файлы" else "Excel файлы",
                        "*.$extension"
                    ))
                    initialFileName = "Отчёт_${LocalDate.now()}.$extension"
                }
                
                val file = fileChooser.showSaveDialog(comboFormat.scene.window)
                if (file != null) {
                    file.writeBytes(bytes)
                    Alert(Alert.AlertType.INFORMATION).apply {
                        title = "Успех"
                        headerText = null
                        contentText = "Отчёт сохранён: ${file.absolutePath}"
                        showAndWait()
                    }
                } else {
                    println("Сохранение отменено пользователем")
                }
            } catch (e: Exception) {
                e.printStackTrace() // Печатаем стек ошибки в консоль для отладки
                Alert(Alert.AlertType.ERROR).apply {
                    title = "Ошибка"
                    headerText = "Ошибка генерации"
                    contentText = e.message
                    showAndWait()
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