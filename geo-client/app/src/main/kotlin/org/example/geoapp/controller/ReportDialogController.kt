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

        println("=== Generate Report Clicked ===")

        println("TOKEN = Bearer $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ReportRequest(
                    reportType = "drilling_completed",
                    reportStart = start.toString(),
                    reportEnd = end.toString(),
                    contractorId = contractorId,
                    areaId = areaId
                )

                val response = MainApp.api
                    .getReportData("Bearer $token", request)
                    .await()

                

                withContext(Dispatchers.Main) {
                    response.rows.forEach {
                        println("${it.boreholeName} | ${it.hValue}")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Alert(Alert.AlertType.ERROR, "Ошибка: ${e.message}").showAndWait()
                    print("Error fetching report data: ")
                    print(e.message)
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