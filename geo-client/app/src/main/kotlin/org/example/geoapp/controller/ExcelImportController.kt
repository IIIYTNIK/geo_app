package org.example.geoapp.controller

import com.example.geoapp.api.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference
import org.example.geoapp.MainApp
import org.example.geoapp.util.DbField
import org.example.geoapp.util.CorrectionRow
import org.example.geoapp.util.WorkingParser
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DynamicRow(val rowData: List<String>, val originalRowIndex: Int)

class ExcelImportController {

    @FXML private lateinit var fileInfoLabel: Label
    
    @FXML private lateinit var areaCombo: ComboBox<RefArea>
    @FXML private lateinit var workTypeCombo: ComboBox<RefWorkType>
    @FXML private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML private lateinit var geologistCombo: ComboBox<RefGeologist>
    @FXML private lateinit var drillingRigCombo: ComboBox<RefDrillingRig>

    @FXML private lateinit var sheetCombo: ComboBox<String>
    @FXML private lateinit var startRowSpinner: Spinner<Int>
    @FXML private lateinit var previewTable: TableView<DynamicRow>
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var importButton: Button

    private lateinit var token: String
    private var workbook: Workbook? = null
    private var onCompleteCallback: () -> Unit = {}
    private val api = MainApp.api

    private val columnMapping = mutableMapOf<Int, ComboBox<DbField>>()
    private val excelData = mutableListOf<DynamicRow>()

    @FXML
    fun initialize() {
        startRowSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 2)
        startRowSpinner.isEditable = true // Можно вводить текст ручками

        // Слушатель: при смене подрядчика загружаем геологов
        contractorCombo.selectionModel.selectedItemProperty().addListener { _, _, newContractor ->
            loadGeologistsForContractor(newContractor?.id)
        }
    }

    fun initData(token: String, file: File, onComplete: () -> Unit) {
        this.token = token
        this.onCompleteCallback = onComplete
        fileInfoLabel.text = "Файл: ${file.name}"
        
        loadReferences()
        openWorkbook(file)
    }

    private fun loadReferences() {
        runOnFx {
            try {
                val areas = api.getAreas().await()
                val workTypes = api.getWorkTypes().await()
                val contractors = api.getContractors().await()
                val drillingRigs = api.getDrillingRigs().await()

                areaCombo.items = FXCollections.observableArrayList(areas)
                areaCombo.setupNameConverter { it.name }

                workTypeCombo.items = FXCollections.observableArrayList(workTypes)
                workTypeCombo.setupNameConverter { it.name }

                contractorCombo.items = FXCollections.observableArrayList(contractors)
                contractorCombo.setupNameConverter { it.name }

                drillingRigCombo.items = FXCollections.observableArrayList(drillingRigs)
                drillingRigCombo.setupNameConverter { it.name }

            } catch (e: Exception) {
                statusLabel.text = "Ошибка загрузки справочников: ${e.message}"
            }
        }
    }

    private fun loadGeologistsForContractor(contractorId: Long?) {
        if (contractorId == null) {
            geologistCombo.items.clear()
            geologistCombo.value = null
            return
        }
        runOnFx {
            try {
                val geologists = api.getGeologistsByContractor("Bearer $token", contractorId).await()
                geologistCombo.items.setAll(geologists)
                geologistCombo.setupNameConverter { it.name }
            } catch (e: Exception) {
                geologistCombo.items.clear()
            }
        }
    }

    private fun openWorkbook(file: File) {
        statusLabel.text = "Открытие файла..."
        runOnFx {
            try {
                withContext(Dispatchers.IO) { workbook = WorkbookFactory.create(file) }
                val sheetNames = (0 until (workbook?.numberOfSheets ?: 0)).map { workbook!!.getSheetName(it) }
                sheetCombo.items = FXCollections.observableArrayList(sheetNames)
                sheetCombo.selectionModel.selectedItemProperty().addListener { _, _, _ -> loadSheetData() }
                if (sheetNames.isNotEmpty()) sheetCombo.selectionModel.select(0)
                
                statusLabel.text = "Укажите поля базы над нужными колонками."
                importButton.isDisable = false
            } catch (e: Exception) { statusLabel.text = "Ошибка: ${e.message}" }
        }
    }

    private fun loadSheetData() {
        val sheetIndex = sheetCombo.selectionModel.selectedIndex
        if (sheetIndex < 0 || workbook == null) return

        val sheet = workbook!!.getSheetAt(sheetIndex)
        val formatter = DataFormatter()
        val evaluator = workbook!!.creationHelper.createFormulaEvaluator()

        excelData.clear()
        var maxCols = 0

        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val lastCol = row.lastCellNum.toInt()
            if (lastCol > maxCols) maxCols = lastCol

            val rowValues = (0 until lastCol).map { c ->
                val cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                formatter.formatCellValue(cell, evaluator).trim() 
            }
            excelData.add(DynamicRow(rowValues, i + 1))
        }

        buildDynamicTable(maxCols)
        previewTable.items = FXCollections.observableArrayList(excelData)
    }

    private fun buildDynamicTable(colCount: Int) {
        previewTable.columns.clear()
        columnMapping.clear()
        val dbFields = FXCollections.observableArrayList(*DbField.values())

        // 1. КОЛОНКА С НОМЕРОМ СТРОКИ
        val rowNumCol = TableColumn<DynamicRow, String>("№ стр.")
        rowNumCol.setCellValueFactory { SimpleStringProperty(it.value.originalRowIndex.toString()) }
        rowNumCol.prefWidth = 60.0
        rowNumCol.isSortable = false
        rowNumCol.style = "-fx-alignment: CENTER; -fx-background-color: #f0f0f0; -fx-font-weight: bold;"
        previewTable.columns.add(rowNumCol)

        // 2. ДИНАМИЧЕСКИЕ КОЛОНКИ EXCEL
        for (i in 0 until colCount) {
            val col = TableColumn<DynamicRow, String>()
            val headerLabel = Label("Столбец ${CellReference.convertNumToColString(i)}")
            val combo = ComboBox<DbField>()
            combo.items = dbFields
            combo.value = DbField.IGNORE
            combo.prefWidth = 140.0
            combo.converter = object : javafx.util.StringConverter<DbField>() {
                override fun toString(obj: DbField?) = obj?.title ?: ""
                override fun fromString(string: String) = dbFields.find { it.title == string }
            }
            
            // ЛОГИКА: ЗАЩИТА ОТ ДУБЛИРОВАНИЯ КОЛОНОК
            combo.valueProperty().addListener { _, _, newValue ->
                if (newValue != null && newValue != DbField.IGNORE) {
                    for ((otherIdx, otherCombo) in columnMapping) {
                        if (otherIdx != i && otherCombo.value == newValue) {
                            otherCombo.value = DbField.IGNORE // Сбрасываем дубликат
                        }
                    }
                }
            }

            columnMapping[i] = combo
            col.graphic = VBox(5.0, headerLabel, combo).apply { alignment = javafx.geometry.Pos.CENTER }
            col.setCellValueFactory { SimpleStringProperty(it.value.rowData.getOrNull(i) ?: "") }
            col.isSortable = false
            previewTable.columns.add(col)
        }
    }

    @FXML fun onAutoMap() {
        for ((colIndex, combo) in columnMapping) {
            var bestField = DbField.IGNORE
            for (row in excelData.take(20)) {
                val cellValue = row.rowData.getOrNull(colIndex)?.lowercase() ?: continue
                val matchedField = DbField.values().find { 
                    it != DbField.IGNORE && (cellValue == it.title.lowercase() || cellValue.contains(it.title.lowercase())) 
                }
                if (matchedField != null) { bestField = matchedField; break }
            }
            combo.value = bestField
        }
    }

    @FXML fun onImport() {
        // Обязательно "комитим" значение спиннера, если юзер ввел число ручками, но не нажал Enter
        startRowSpinner.increment(0) 
        
        val startRow = startRowSpinner.value
        val validWorkings = mutableListOf<Working>()
        val invalidRows = mutableListOf<CorrectionRow>()

        // Считываем выбранные справочники
        val selectedArea = areaCombo.selectionModel.selectedItem
        val selectedWorkType = workTypeCombo.selectionModel.selectedItem
        val selectedContractor = contractorCombo.selectionModel.selectedItem
        val selectedGeologist = geologistCombo.selectionModel.selectedItem
        val selectedDrillingRig = drillingRigCombo.selectionModel.selectedItem

        for (row in excelData) {
            if (row.originalRowIndex < startRow) continue

            val valuesMap = mutableMapOf<DbField, String>()
            for ((colIdx, combo) in columnMapping) {
                val field = combo.value
                if (field != DbField.IGNORE) {
                    valuesMap[field] = row.rowData.getOrNull(colIdx) ?: ""
                }
            }

            if (valuesMap[DbField.NUMBER].isNullOrBlank()) continue

            try {
                // Парсим сырые данные и прикрепляем глобальные справочники
                val workingBase = WorkingParser.parse(valuesMap)
                val working = workingBase.copy(
                    area = selectedArea,
                    workType = selectedWorkType,
                    contractor = selectedContractor,
                    geologist = selectedGeologist,
                    drillingRig = selectedDrillingRig
                )
                validWorkings.add(working)
            } catch (e: Exception) {
                invalidRows.add(CorrectionRow(row.originalRowIndex, e.message ?: "Ошибка формата", valuesMap))
            }
        }

        importButton.isDisable = true
        statusLabel.text = "Отправка на сервер..."

        runOnFx {
            try {
                if (validWorkings.isNotEmpty()) {
                    api.createWorkingsBatch("Bearer $token", validWorkings).await()
                }

                if (invalidRows.isNotEmpty()) {
                    showCorrectionWindow(invalidRows)
                } else {
                    onCompleteCallback()
                    close()
                }
            } catch (e: Exception) {
                statusLabel.text = "Ошибка сервера: ${e.message}"
                importButton.isDisable = false
            }
        }
    }

    private fun showCorrectionWindow(invalidRows: List<CorrectionRow>) {
        val loader = FXMLLoader(javaClass.getResource("/importCorrection.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ImportCorrectionController>()
        
        val mappedFields = columnMapping.values.map { it.value }.filter { it != DbField.IGNORE }.distinct()
        
        controller.initData(token, invalidRows, mappedFields) {
            onCompleteCallback()
            close()
        }

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(previewTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Исправление ошибок импорта"
        stage.showAndWait()
    }

    @FXML fun onCancel() {
        workbook?.close()
        close()
    }

    private fun close() { (previewTable.scene.window as Stage).close() }

    // Вспомогательная функция для ComboBox
    private fun <T> ComboBox<T>.setupNameConverter(extractor: (T) -> String) {
        converter = object : javafx.util.StringConverter<T>() {
            override fun toString(obj: T?): String = obj?.let(extractor) ?: ""
            override fun fromString(string: String): T? {
                return items.find { extractor(it) == string }
            }
        }
    }
}