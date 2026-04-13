package org.example.geoapp.controller

import com.example.geoapp.api.Working
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.CorrectionRow
import org.example.geoapp.util.DbField
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx

class ImportCorrectionController {

    @FXML private lateinit var adminPanel: HBox
    @FXML private lateinit var correctionTable: TableView<CorrectionRow>
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var retryButton: Button  // если нужно, но в FXML нет, можно добавить

    private lateinit var token: String
    private lateinit var parentController: ExcelImportController
    private var onCompleteCallback: () -> Unit = {}
    private var onCancelCallback: () -> Unit = {}
    private var mappedFields: List<DbField> = emptyList()

    private var validWorkingsToSend: List<Working> = emptyList()
    private var nextOrderNumStart: Int = 1

    fun initData(
        token: String,
        role: String,
        parent: ExcelImportController,
        errors: List<CorrectionRow>,
        mappedFields: List<DbField>,
        validWorkings: List<Working>,   
        nextOrderNum: Int, 
        onComplete: () -> Unit,
        onCancel: () -> Unit
    ) {
        this.token = token
        this.parentController = parent
        this.validWorkingsToSend = validWorkings.toList()
        this.nextOrderNumStart = nextOrderNum
        this.onCompleteCallback = onComplete
        this.onCancelCallback = onCancel
        this.mappedFields = mappedFields


        // Панель админа видна только для ROLE_ADMIN
        adminPanel.isVisible = role == "ROLE_ADMIN"
        adminPanel.isManaged = role == "ROLE_ADMIN"

        buildTable()
        correctionTable.items = FXCollections.observableArrayList(errors)
        statusLabel.text = "Ошибок найдено: ${errors.size}"
    }

    private fun buildTable() {
        correctionTable.columns.clear()

        val rowCol = TableColumn<CorrectionRow, String>("Строка")
        rowCol.setCellValueFactory { SimpleStringProperty(it.value.originalRowIndex.toString()) }
        rowCol.prefWidth = 60.0
        correctionTable.columns.add(rowCol)

        val errCol = TableColumn<CorrectionRow, String>("Описание ошибки")
        errCol.setCellValueFactory { SimpleStringProperty(it.value.errorMsg) }
        errCol.style = "-fx-text-fill: red; -fx-font-weight: bold;"
        errCol.prefWidth = 250.0
        correctionTable.columns.add(errCol)

        for (field in mappedFields) {
            val col = TableColumn<CorrectionRow, String>(field.title)
            col.setCellValueFactory { SimpleStringProperty(it.value.rawValues[field] ?: "") }

            if (field.isReference) {
                val options = getOptionsForField(field)
                col.cellFactory = ComboBoxTableCell.forTableColumn(*options.toTypedArray())
            } else {
                col.cellFactory = TextFieldTableCell.forTableColumn()
            }

            col.setOnEditCommit { event ->
                val row = event.tableView.items[event.tablePosition.row]
                row.rawValues[field] = event.newValue
            }
            col.prefWidth = 120.0
            correctionTable.columns.add(col)
        }
    }

    private fun getOptionsForField(field: DbField): List<String> {
        return when (field) {
            DbField.AREA -> parentController.cacheAreas.map { it.name }
            DbField.WORK_TYPE -> parentController.cacheWorkTypes.map { it.name }
            DbField.CONTRACTOR -> parentController.cacheContractors.map { it.name }
            DbField.GEOLOGIST -> parentController.cacheGeologists.map { it.name }
            DbField.DRILLING_RIG -> parentController.cacheDrillingRigs.map { it.name }
            else -> emptyList()
        }
    }

        // КНОПКА: Обновить и перепроверить
    @FXML fun onRefreshAndRetry() {
        statusLabel.text = "Синхронизация справочников..."
        parentController.loadReferences {
            parentController.updateNextOrderNum()
            // Перестраиваем таблицу (чтобы обновить выпадающие списки)
            buildTable()
            onRetryCheck()
        }
    }

    // КНОПКА 1: Выйти обратно в Excel
    @FXML fun onExitBack() {
        onCancelCallback.invoke()
        close()
    }

    // КНОПКА 2: Пропустить ошибки (импорт только целых)
    @FXML fun onSkipAndImport() {
        val readyToImport = validWorkingsToSend.toMutableList()
        for (item in correctionTable.items) {
            try {
                val w = parentController.validateAndParse(item.rawValues)
                if (w != null) readyToImport.add(w)
            } catch (e: Exception) {
                // Ошибка – пропускаем строку, не добавляем в импорт
                // Ничего не выводим в консоль, просто игнорируем
            }
        }
        if (readyToImport.isEmpty()) {
            statusLabel.text = "Нет данных для отправки."
            return
        }
        sendData(readyToImport)
    }

    // КНОПКА 3: Повторить импорт (перепроверка)
    @FXML fun onRetryCheck() {
        val stillInvalid = mutableListOf<CorrectionRow>()
        val fixed = mutableListOf<Working>()
        
        // Валидные строки, которые уже были, остаются валидными
        fixed.addAll(validWorkingsToSend)
        
        // Проверяем строки из таблицы коррекции
        for (item in correctionTable.items) {
            try {
                val w = parentController.validateAndParse(item.rawValues)
                if (w != null) {
                    fixed.add(w)          // исправленная строка – добавляем к отправке
                } else {
                    stillInvalid.add(item) // строка пустая или не содержит данных
                }
            } catch (e: Exception) {
                // Ошибка – строка остаётся в таблице коррекции
                stillInvalid.add(item)
            }
        }
        
        if (stillInvalid.isEmpty()) {
            // Все ошибки исправлены – отправляем всё
            sendData(fixed)
        } else {
            // Обновляем таблицу, показывая только оставшиеся ошибки
            correctionTable.items.setAll(stillInvalid)
            statusLabel.text = "Осталось ошибок: ${stillInvalid.size}. Исправьте их или нажмите 'Пропустить'."
            // Сохраняем все валидные строки (включая только что исправленные) для будущей отправки
            validWorkingsToSend = fixed
        }
    }

    private fun sendData(workings: List<Working>) {
        runOnFx {
            try {
                statusLabel.text = "Сохранение..."
                MainApp.api.createBatch("Bearer $token", workings).await()
                onCompleteCallback.invoke()
                close()
            } catch (e: Exception) {
                statusLabel.text = "Ошибка сервера: ${e.message}"
            }
        }
    }

    private fun openRefEditor(type: RefType) {
        val loader = FXMLLoader(javaClass.getResource("/referenceEditor.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ReferenceEditorController>()
        controller.initData(token, type)
        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(correctionTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Справочник: ${type.title}"
        stage.showAndWait()
        parentController.loadReferences() // обновить справочники после закрытия
    }

    @FXML fun openAreas() = openRefEditor(RefType.AREA)
    @FXML fun openWorkTypes() = openRefEditor(RefType.WORK_TYPE)
    @FXML fun openContractors() = openRefEditor(RefType.CONTRACTOR)
    @FXML fun openGeologists() = openRefEditor(RefType.GEOLOGIST)
    @FXML fun openDrillingRigs() = openRefEditor(RefType.DRILLING_RIG)

    private fun close() {
        (correctionTable.scene.window as Stage).close()
    }

}