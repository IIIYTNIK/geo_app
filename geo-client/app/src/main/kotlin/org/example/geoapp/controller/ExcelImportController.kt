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
import java.math.BigDecimal

class DynamicRow(val rowData: List<String>, val originalRowIndex: Int)

class ExcelImportController {

    private var existingWorkings: List<Working> = emptyList()
    var nextOrderNum: Int = 1

    @FXML private lateinit var fileInfoLabel: Label
    @FXML private lateinit var sheetCombo: ComboBox<String>
    @FXML private lateinit var startRowSpinner: Spinner<Int>
    @FXML private lateinit var previewTable: TableView<DynamicRow>
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var importButton: Button

    @FXML private lateinit var importAreaCombo: ComboBox<RefArea>
    private var isProjectImport: Boolean = false

    private lateinit var token: String
    private lateinit var userRole: String
    private var workbook: Workbook? = null
    private var onCompleteCallback: () -> Unit = {}
    private val api = MainApp.api

    val columnMapping = mutableMapOf<Int, ComboBox<DbField>>()
    private val excelData = mutableListOf<DynamicRow>()

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

    fun initData(token: String, role: String, file: File, isProject: Boolean, onComplete: () -> Unit) {
        this.token = token
        this.userRole = role
        this.isProjectImport = isProject
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
                
                // Загружаем все выработки, чтобы определить следующий orderNum
                existingWorkings = api.getWorkings("Bearer $token").await()
                val maxOrderNum = existingWorkings.mapNotNull { it.orderNum }.maxOrNull() ?: 0
                nextOrderNum = maxOrderNum + 1

                // Настраиваем комбобокс участков
                importAreaCombo.items = FXCollections.observableArrayList(cacheAreas)
                importAreaCombo.converter = object : javafx.util.StringConverter<RefArea>() {
                    override fun toString(obj: RefArea?) = obj?.name ?: ""
                    override fun fromString(string: String) = cacheAreas.find { it.name == string }
                }

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
                when {
                    cell == null -> ""
                    cell.cellType == CellType.NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            cell.localDateTimeCellValue.toLocalDate().toString()
                        } else {
                            BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
                        }
                    }
                    else -> formatter.formatCellValue(cell, evaluator).trim()
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

        val availableFields = DbField.values().filter { it != DbField.AREA }.toTypedArray()
        val dbFields = FXCollections.observableArrayList(*availableFields)
        
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
            
            // combo.valueProperty().addListener { _, _, newValue ->
            //     if (newValue != null && newValue != DbField.IGNORE) {
            //         for ((otherIdx, otherCombo) in columnMapping) {
            //             if (otherIdx != i && otherCombo.value == newValue) {
            //                 otherCombo.value = DbField.IGNORE
            //             }
            //         }
            //     }
            // }

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
        if (importAreaCombo.value == null) {
            statusLabel.text = "ОШИБКА: Сначала выберите Участок в верхней панели!"
            return
        }

        // Проверка уникальности выбранных полей
        val selectedFields = columnMapping.values.map { it.value }.filter { it != DbField.IGNORE }
        val duplicates = selectedFields.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            val duplicateNames = duplicates.joinToString(", ") { it.title }
            statusLabel.text = "ОШИБКА: Поля '$duplicateNames' назначены нескольким колонкам. Устраните дублирование."
            return
        }
                
        startRowSpinner.increment(0)
        val startRow = startRowSpinner.value
        
        // 1. Парсим все строки Excel, получаем валидные и невалидные записи
        val validWorkings = mutableListOf<Working>()
        val invalidRows = mutableListOf<CorrectionRow>()
        var currentOrderNum = nextOrderNum

        for (row in excelData) {
            if (row.originalRowIndex < startRow) continue

            val rawValues = mutableMapOf<DbField, String>()
            var hasAnyData = false

            for ((colIdx, combo) in columnMapping) {
                if (combo.value != DbField.IGNORE) {
                    val cellValue = row.rowData.getOrNull(colIdx) ?: ""
                    rawValues[combo.value] = cellValue
                    if (cellValue.isNotBlank()) hasAnyData = true
                }
            }

            if (!hasAnyData) continue

            try {
                val working = validateAndParse(rawValues)
                if (working == null) continue
                validWorkings.add(working)
                currentOrderNum++
            } catch (e: Exception) {
                invalidRows.add(CorrectionRow(row.originalRowIndex, e.message ?: "Ошибка", rawValues))
            }
        }

        // 2. Обрабатываем конфликты (дубликаты)
        val conflicts = findConflicts(validWorkings)
        var finalToCreate: List<Working> = emptyList()
        var finalToUpdate: List<Working> = emptyList()

        if (conflicts.isNotEmpty()) {
            val action = showConflictResolutionDialog(conflicts.size)
            if (action == null) {
                statusLabel.text = "Импорт отменён пользователем"
                return
            }
            val (toCreate, toUpdate) = applyConflictResolution(validWorkings, conflicts, action)
            finalToCreate = toCreate
            finalToUpdate = toUpdate
        } else {
            finalToCreate = validWorkings
            finalToUpdate = emptyList()
        }

        // 3. Выполняем импорт в корутине
        importButton.isDisable = true
        statusLabel.text = "Обработка данных..."

        runOnFx {
            try {
                // Создаём новые записи
                if (finalToCreate.isNotEmpty()) {
                    api.createWorkingsBatch("Bearer $token", finalToCreate).await()
                }
                // Обновляем существующие
                for (w in finalToUpdate) {
                    api.updateWorking("Bearer $token", w.id, w).await()
                }

                // Обновляем nextOrderNum после импорта
                updateNextOrderNum()

                // Если есть ошибки, показываем окно коррекции
                if (invalidRows.isNotEmpty()) {
                    showCorrectionWindow(invalidRows)
                    statusLabel.text = "Готово. Обработайте ошибки."
                    importButton.isDisable = false
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

    fun validateAndParse(raw: Map<DbField, String>): Working? {
        fun getNum(field: DbField): Double? {
            val rawStr = raw[field]?.replace(",", ".")?.trim()
            if (rawStr.isNullOrEmpty()) return null
            
            // Очищаем строку от лишних символов (невидимые пробелы и т.д.), оставляя минус и точку
            val cleanStr = rawStr.replace(Regex("[^0-9.\\-]"), "")
            val value = cleanStr.toDoubleOrNull() ?: throw Exception("Ожидается число для '${field.title}' (введено: '$rawStr')")

           return (value)
            
        }
        
        fun getStr(field: DbField): String? = raw[field]?.trim()?.ifEmpty { null }

        fun getSafeStr(name: String): String? {
            val key = raw.keys.find { it.name == name } ?: return null
        val value = raw[key]?.trim()?.replace("\\s+".toRegex(), " ")
        return if (value.isNullOrEmpty()) null else value
        }

        fun getSafeBool(name: String): Boolean {
            val str = getSafeStr(name)?.lowercase()
            return str == "да" || str == "true" || str == "1" || str == "+" || str == "yes"
        }

        // УМНЫЙ ПАРСЕР ДАТ
        fun getDateStr(field: DbField): String? {
            val str = getStr(field) ?: return null

            // Проверка: если Excel прислал число (например, 45226)
            val excelSerialDate = str.toDoubleOrNull()
            if (excelSerialDate != null) {
                try {
                    // Превращаем число Excel в стандартную дату Java
                    val javaDate = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(excelSerialDate)
                    return java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                        .format(javaDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                } catch (e: Exception) { /* если не вышло, пробуем текстовые форматы ниже */ }
            }

            // Список текстовых форматов
            val formats = listOf(
                "yyyy-MM-dd", "dd.MM.yyyy", "dd.MM.yy", 
                "MM/dd/yy", "dd/MM/yy", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy.MM.dd"
            )
            for (pattern in formats) {
                try {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                    val date = java.time.LocalDate.parse(str, formatter)
                    return date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) { }
            }
            throw Exception("Неверный формат даты: '$str'. Ожидается ДД.ММ.ГГГГ или числовой формат Excel")
        }

        // println("=== validateAndParse ===")
        // println("raw keys: ${raw.keys.map { it.name }}")
        // println("NUMBER value: ${getSafeStr("NUMBER")}")
        // println("NAME_COMBINED value: ${getSafeStr("NAME_COMBINED")}")

        val number = getSafeStr("NUMBER") ?: ""
        var parsedNumber = number
        var parsedWorkType: RefWorkType? = null

        val combinedName = getSafeStr("NAME_COMBINED")
        // Если оба ключевых поля пустые, считаем строку пустой и пропускаем
        if (number.isEmpty() && (combinedName == null || combinedName.isEmpty())) {
            return null 
        }

        if (combinedName != null && combinedName.isNotEmpty()) {
            // Разбиваем по первому дефису
            val parts = combinedName.split("-", limit = 2)
            if (parts.size == 2) {
                val typePrefix = parts[0].trim()
                parsedNumber = parts[1].trim()
                if (typePrefix.isNotEmpty()) {
                    parsedWorkType = when (typePrefix) {
                        "С", "с" -> cacheWorkTypes.find { it.name.equals("скважина", ignoreCase = true) }
                        "Ш", "ш" -> cacheWorkTypes.find { it.name.equals("шурф", ignoreCase = true) }
                        "Р", "р" -> cacheWorkTypes.find { it.name.equals("расчистка", ignoreCase = true) }
                        else -> cacheWorkTypes.find { it.name.startsWith(typePrefix, ignoreCase = true) }
                    }
                }
            } else {
                parsedNumber = combinedName
            }
        }

        if (parsedNumber.isEmpty()) throw Exception("Обязательное поле Номер выработки пустое")

        val area = importAreaCombo.value ?: throw Exception("Не выбран участок")

        val workTypeName = getStr(DbField.WORK_TYPE)
        val workType = parsedWorkType ?: if (workTypeName != null) cacheWorkTypes.find { it.name.equals(workTypeName, true) } ?: throw Exception("Тип выработки '$workTypeName' не найден") else null

        val rigName = getStr(DbField.DRILLING_RIG)
        val rig = if (rigName != null) {
            cacheDrillingRigs.find { it.name.equals(rigName, true) }
                ?: cacheDrillingRigs.find { it.alias?.equals(rigName, true) == true }
                ?: throw Exception("Буровая '$rigName' не найдена")
        } else null

        val contractorName = getStr(DbField.CONTRACTOR)
        val contractor = if (contractorName != null) cacheContractors.find { it.name.equals(contractorName, true) } ?: throw Exception("Подрядчик '$contractorName' не найден") else null

        val geologistName = getStr(DbField.GEOLOGIST)
        val geologist = if (geologistName != null) {
            val geo = cacheGeologists.find { it.name.equals(geologistName, true) }
                ?: cacheGeologists.find { it.alias?.equals(geologistName, true) == true }
                ?: throw Exception("Геолог '$geologistName' не найден в базе")
            if (contractor != null && geo.contractor?.id != contractor.id) {
                throw Exception("Геолог относится к другому подрядчику")
            }
            geo
        } else null

        val coreRec = getNum(DbField.CORE_RECOVERY)
        if (coreRec != null && (coreRec < 0 || coreRec > 100)) throw Exception("Выход керна должен быть от 0 до 100%")

        if (!isProjectImport) {
            if (contractor == null) throw Exception("Для фактической скважины обязателен 'Подрядчик'")
            if (geologist == null) throw Exception("Для фактической скважины обязателен 'Геолог'")
            if (rig == null) throw Exception("Для фактической скважины обязательна 'Буровая'")
        }

        return Working(
            number = parsedNumber, 
            //orderNum = orderNum,
            area = area, 
            isProject = isProjectImport,
            workType = workType, 
            contractor = contractor, 
            geologist = geologist, 
            drillingRig = rig, 
            structure = getSafeStr("STRUCTURE"),

            plannedX = getNum(DbField.PLANNED_X), 
            plannedY = getNum(DbField.PLANNED_Y), 
            plannedDepth = getNum(DbField.PLANNED_DEPTH), 
            actualX = getNum(DbField.ACTUAL_X), 
            actualY = getNum(DbField.ACTUAL_Y), 
            actualZ = getNum(DbField.ACTUAL_Z),
            actualDepth = getNum(DbField.ACTUAL_DEPTH), 
            coreRecovery = coreRec, 
            casing = getNum(DbField.CASING), 
            
            startDate = getDateStr(DbField.START_DATE), 
            endDate = getDateStr(DbField.END_DATE),

            mmg1Top = getNum(DbField.MMG1_TOP), 
            mmg1Bottom = getNum(DbField.MMG1_BOTTOM),
            mmg2Top = getNum(DbField.MMG2_TOP), 
            mmg2Bottom = getNum(DbField.MMG2_BOTTOM),
            gwAppearLog = getNum(DbField.GW_APPEAR_LOG), 
            gwStableLog = getNum(DbField.GW_STABLE_LOG),

            hasVideo = getSafeBool("HAS_VIDEO"), 
            hasDrilling = getSafeBool("HAS_DRILLING"),
            hasJournal = getSafeBool("HAS_JOURNAL"),
            hasCore = getSafeBool("HAS_CORE"),
            hasStake = getSafeBool("HAS_STAKE"),

            act = getSafeBool("ACT"),
            actNumber = getSafeStr("ACT_NUMBER"), 
            thermalTube = getSafeBool("THERMAL_TUBE"),
            additionalInfo = getSafeStr("ADDITIONAL_INFO") ?: getSafeStr("COMMENT")
        )
    }

    fun updateNextOrderNum() {
        runOnFx {
            try {
                val allWorkings = api.getWorkings("Bearer $token").await()
                existingWorkings = allWorkings
                val maxOrderNum = allWorkings.mapNotNull { it.orderNum }.maxOrNull() ?: 0
                nextOrderNum = maxOrderNum + 1
            } catch (e: Exception) {
                // игнорируем ошибки обновления
            }
        }
    }

    private fun showCorrectionWindow(invalidRows: List<CorrectionRow>) {
        val loader = FXMLLoader(javaClass.getResource("/importCorrection.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ImportCorrectionController>()
        
        val mappedFields = columnMapping.values.map { it.value }.filter { it != DbField.IGNORE }.distinct()
        
        controller.initData(token, userRole, this, invalidRows, mappedFields, nextOrderNum) {
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

    // Метод для поиска конфликтов (не suspend)
    private fun findConflicts(validWorkings: List<Working>): List<Working> {
        val conflicts = mutableListOf<Working>()


        for (w in validWorkings) {
            val exists = existingWorkings.any { 
                it.area?.id == w.area?.id && it.number == w.number 
            }
            if (exists) conflicts.add(w)
        }
        return conflicts
    }

    // Показываем диалог выбора действия (блокирующий, не suspend)
    private fun showConflictResolutionDialog(conflictCount: Int): String? {
        val options = arrayOf("Пропустить дубликаты", "Обновить существующие")
        val dialog = ChoiceDialog("Пропустить дубликаты", *options)
        dialog.title = "Конфликт дубликатов"
        dialog.headerText = "Найдено $conflictCount дублирующихся записей"
        dialog.contentText = "Выберите действие:"
        return dialog.showAndWait().orElse(null)
    }

    // Применяем решение
    private fun applyConflictResolution(
        validWorkings: List<Working>, 
        conflicts: List<Working>, 
        action: String
    ): Pair<List<Working>, List<Working>> {
        return if (action == "Пропустить дубликаты") {
            val toCreate = validWorkings.filter { it !in conflicts }
            Pair(toCreate, emptyList())
        } else { // Обновить существующие
            val toUpdate = mutableListOf<Working>()
            val toCreate = mutableListOf<Working>()
            for (w in validWorkings) {
                val existing = existingWorkings.find { 
                    it.area?.id == w.area?.id && it.number == w.number 
                }
                if (existing != null) {
                    val updated = w.copy(
                        id = existing.id,
                        orderNum = existing.orderNum
                        // остальные поля уже из w (кроме id и orderNum)
                    )
                    toUpdate.add(updated)
                } else {
                    toCreate.add(w)
                }
            }
            Pair(toCreate, toUpdate)
        }
    }

}