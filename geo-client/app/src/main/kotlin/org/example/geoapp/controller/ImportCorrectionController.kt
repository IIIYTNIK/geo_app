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
    @FXML private lateinit var retryButton: Button

    private lateinit var token: String
    private lateinit var parentController: ExcelImportController
    private var onAllCompletedCallback: () -> Unit = {}
    private val api = MainApp.api

    fun initData(token: String, role: String, parent: ExcelImportController, errors: List<CorrectionRow>, mappedFields: List<DbField>, onCompleted: () -> Unit) {
        this.token = token
        this.parentController = parent
        this.onAllCompletedCallback = onCompleted

        // Панель админа
        if (role == "ROLE_ADMIN") {
            adminPanel.isVisible = true
            adminPanel.isManaged = true
        }

        buildTable(mappedFields)
        correctionTable.items = FXCollections.observableArrayList(errors)
        statusLabel.text = "Ошибок найдено: ${errors.size}"
    }

    private fun buildTable(mappedFields: List<DbField>) {
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
            
            // Если поле справочное, даем только выпадающий список существующих значений
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

    @FXML fun onRefreshAndRetry() {
        statusLabel.text = "Синхронизация справочников..."
        retryButton.isDisable = true
        // Обновляем кэш и перестраиваем таблицу (чтобы в комбобоксах появились новые имена)
        parentController.loadReferences {
            val mappedFields = parentController.columnMapping.values.map { it.value }.filter { it != DbField.IGNORE }.distinct()
            buildTable(mappedFields)
            onRetry()
        }
    }

    @FXML fun onRetry() {
        val stillInvalid = mutableListOf<CorrectionRow>()
        val validWorkings = mutableListOf<Working>()

        for (row in correctionTable.items) {
            try {
                val working = parentController.validateAndParse(row.rawValues)
                validWorkings.add(working)
            } catch (e: Exception) {
                row.errorMsg = e.message ?: "Ошибка"
                stillInvalid.add(row)
            }
        }

        retryButton.isDisable = true
        statusLabel.text = "Отправка..."

        runOnFx {
            try {
                if (validWorkings.isNotEmpty()) {
                    api.createWorkingsBatch("Bearer $token", validWorkings).await()
                }

                if (stillInvalid.isNotEmpty()) {
                    correctionTable.items = FXCollections.observableArrayList(stillInvalid)
                    correctionTable.refresh()
                    statusLabel.text = "Осталось ошибок: ${stillInvalid.size}"
                    retryButton.isDisable = false
                } else {
                    onAllCompletedCallback()
                    close()
                }
            } catch (e: Exception) {
                statusLabel.text = "Ошибка сервера: ${e.message}"
                retryButton.isDisable = false
            }
        }
    }

    // Вызовы стандартных справочников
    @FXML fun openAreas() = openRefEditor(RefType.AREA)
    @FXML fun openWorkTypes() = openRefEditor(RefType.WORK_TYPE)
    @FXML fun openContractors() = openRefEditor(RefType.CONTRACTOR)
    @FXML fun openGeologists() = openRefEditor(RefType.GEOLOGIST)
    @FXML fun openDrillingRigs() = openRefEditor(RefType.DRILLING_RIG)

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
    }

    @FXML fun onCancel() { onAllCompletedCallback(); close() }
    private fun close() { (correctionTable.scene.window as Stage).close() }
}