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
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DynamicRow(val rowData: List<String>, val originalRowIndex: Int)

class ExcelImportController {

    @FXML private lateinit var fileInfoLabel: Label
    @FXML private lateinit var sheetCombo: ComboBox<String>
    @FXML private lateinit var startRowSpinner: Spinner<Int>
    @FXML private lateinit var previewTable: TableView<DynamicRow>
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var importButton: Button

    private lateinit var token: String
    private lateinit var userRole: String
    private var workbook: Workbook? = null
    private var onCompleteCallback: () -> Unit = {}
    private val api = MainApp.api

    // Делаем val, чтобы был доступ из ImportCorrectionController
    val columnMapping = mutableMapOf<Int, ComboBox<DbField>>()
    private val excelData = mutableListOf<DynamicRow>()

    // Кэш для проверки справочников (val, чтобы дочернее окно их видело)
    var cacheAreas = listOf<RefArea>()
    var cacheWorkTypes = listOf<RefWorkType>()
    var cacheContractors = listOf<RefContractor>()
    var cacheGeologists = listOf<RefGeologist>()
    var cacheDrillingRigs = listOf<RefDrillingRig>()

    @FXML
    fun initialize() {
        startRowSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 2)
        startRowSpinner.isEditable = true
    }

    fun initData(token: String, role: String, file: File, onComplete: () -> Unit) {
        this.token = token
        this.userRole = role
        this.onCompleteCallback = onComplete
        fileInfoLabel.text = "Файл: ${file.name}"
        
        loadReferences { openWorkbook(file) }
    }

    fun loadReferences(onLoaded: () -> Unit = {}) {
        runOnFx {
            try {
                cacheAreas = api.getAreas().await()
                cacheWorkTypes = api.getWorkTypes().await()
                cacheContractors = api.getContractors().await()
                cacheGeologists = api.getGeologists().await()
                cacheDrillingRigs = api.getDrillingRigs().await()
                onLoaded()
            } catch (e: Exception) {
                statusLabel.text = "Ошибка загрузки справочников: ${e.message}"
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
                try {
                    cell?.let { formatter.formatCellValue(it, evaluator) }?.trim() ?: ""
                } catch (e: Exception) {
                    // Fallback если формула битая
                    cell?.let { formatter.formatCellValue(it) }?.trim() ?: ""
                }
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

        val rowNumCol = TableColumn<DynamicRow, String>("№ стр.")
        rowNumCol.setCellValueFactory { SimpleStringProperty(it.value.originalRowIndex.toString()) }
        rowNumCol.prefWidth = 60.0
        rowNumCol.isSortable = false
        rowNumCol.style = "-fx-alignment: CENTER; -fx-background-color: #f0f0f0; -fx-font-weight: bold;"
        previewTable.columns.add(rowNumCol)

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
            
            // Защита от дублирования выбора колонок
            combo.valueProperty().addListener { _, _, newValue ->
                if (newValue != null && newValue != DbField.IGNORE) {
                    for ((otherIdx, otherCombo) in columnMapping) {
                        if (otherIdx != i && otherCombo.value == newValue) {
                            otherCombo.value = DbField.IGNORE
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
        startRowSpinner.increment(0) // Заставляет Spinner зафиксировать введенное руками число
        val startRow = startRowSpinner.value
        
        val validWorkings = mutableListOf<Working>()
        val invalidRows = mutableListOf<CorrectionRow>()

        for (row in excelData) {
            if (row.originalRowIndex < startRow) continue

            val rawValues = mutableMapOf<DbField, String>()
            for ((colIdx, combo) in columnMapping) {
                if (combo.value != DbField.IGNORE) {
                    rawValues[combo.value] = row.rowData.getOrNull(colIdx) ?: ""
                }
            }

            if (rawValues[DbField.NUMBER].isNullOrBlank()) continue

            try {
                val working = validateAndParse(rawValues)
                validWorkings.add(working)
            } catch (e: Exception) {
                invalidRows.add(CorrectionRow(row.originalRowIndex, e.message ?: "Ошибка", rawValues))
            }
        }

        importButton.isDisable = true
        statusLabel.text = "Обработка данных..."

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

    // Умная валидация строки с учетом справочников
    fun validateAndParse(raw: Map<DbField, String>): Working {
        fun getNum(field: DbField): Double? {
            val str = raw[field]?.replace(",", ".")?.trim()
            if (str.isNullOrEmpty()) return null
            return str.toDoubleOrNull() ?: throw Exception("Ожидается число для '${field.title}'")
        }
        fun getStr(field: DbField): String? = raw[field]?.trim()?.ifEmpty { null }
        
        val number = getStr(DbField.NUMBER) ?: throw Exception("Номер скважины обязателен")
        
        // 1. Участок, Тип, Буровая
        val areaName = getStr(DbField.AREA)
        val area = if (areaName != null) {
            cacheAreas.find { it.name.equals(areaName, true) } 
                ?: throw Exception("Участок '$areaName' не найден")
        } else null

        val workTypeName = getStr(DbField.WORK_TYPE)
        val workType = if (workTypeName != null) {
            cacheWorkTypes.find { it.name.equals(workTypeName, true) } 
                ?: throw Exception("Тип выработки '$workTypeName' не найден")
        } else null

        val rigName = getStr(DbField.DRILLING_RIG)
        val rig = if (rigName != null) {
            cacheDrillingRigs.find { it.name.equals(rigName, true) } 
                ?: throw Exception("Буровая '$rigName' не найдена")
        } else null

        // 2. Строгая проверка: Подрядчик -> Геолог
        val contractorName = getStr(DbField.CONTRACTOR)
        val contractor = if (contractorName != null) {
            cacheContractors.find { it.name.equals(contractorName, true) } 
                ?: throw Exception("Подрядчик '$contractorName' не найден в базе")
        } else null

        val geologistName = getStr(DbField.GEOLOGIST)
        val geologist = if (geologistName != null) {
            val geo = cacheGeologists.find { it.name.equals(geologistName, true) }
                ?: throw Exception("Геолог '$geologistName' не найден в базе")
            
            // Проверка принадлежности геолога к подрядчику
            if (contractor != null && geo.contractor?.id != contractor.id) {
                val realContractor = geo.contractor?.name ?: "Без подрядчика"
                throw Exception("Геолог '${geo.name}' привязан к '${realContractor}', а не к '${contractor.name}'")
            }
            geo
        } else null

        val coreRec = getNum(DbField.CORE_RECOVERY)
        if (coreRec != null && (coreRec < 0 || coreRec > 100)) throw Exception("Выход керна от 0 до 100")

        return Working(
            number = number, area = area, workType = workType, contractor = contractor, 
            geologist = geologist, drillingRig = rig, plannedX = getNum(DbField.PLANNED_X),
            plannedY = getNum(DbField.PLANNED_Y), plannedZ = getNum(DbField.PLANNED_Z),
            actualX = getNum(DbField.ACTUAL_X), actualY = getNum(DbField.ACTUAL_Y), actualZ = getNum(DbField.ACTUAL_Z),
            depth = getNum(DbField.DEPTH), coreRecovery = coreRec, casing = getStr(DbField.CASING),
            startDate = getStr(DbField.START_DATE), endDate = getStr(DbField.END_DATE),
            mmg1Top = getNum(DbField.MMG1_TOP), mmg1Bottom = getNum(DbField.MMG1_BOTTOM),
            mmg2Top = getNum(DbField.MMG2_TOP), mmg2Bottom = getNum(DbField.MMG2_BOTTOM),
            gwAppearLog = getNum(DbField.GW_APPEAR_LOG), gwStableLog = getNum(DbField.GW_STABLE_LOG),
            gwStableAbs = getNum(DbField.GW_STABLE_ABS), gwStableRel = getNum(DbField.GW_STABLE_REL),
            gwStableAbsFinal = getNum(DbField.GW_STABLE_ABS_FINAL), act = getStr(DbField.ACT),
            actNumber = getStr(DbField.ACT_NUMBER), thermalTube = getStr(DbField.THERMAL_TUBE),
            additionalInfo = getStr(DbField.ADDITIONAL_INFO)
        )
    }

    private fun showCorrectionWindow(invalidRows: List<CorrectionRow>) {
        val loader = FXMLLoader(javaClass.getResource("/importCorrection.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ImportCorrectionController>()
        
        val mappedFields = columnMapping.values.map { it.value }.filter { it != DbField.IGNORE }.distinct()
        
        controller.initData(token, userRole, this, invalidRows, mappedFields) {
            onCompleteCallback()
            close()
        }

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(previewTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Ошибки импорта"
        stage.showAndWait()
    }

    @FXML fun onCancel() { workbook?.close(); close() }
    private fun close() { (previewTable.scene.window as Stage).close() }
}