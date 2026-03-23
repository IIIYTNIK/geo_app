package org.example.geoapp.controller

import com.example.geoapp.api.Working
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.CorrectionRow
import org.example.geoapp.util.DbField
import org.example.geoapp.util.WorkingParser
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx

class ImportCorrectionController {

    @FXML private lateinit var correctionTable: TableView<CorrectionRow>
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var retryButton: Button

    private lateinit var token: String
    private var onAllCompletedCallback: () -> Unit = {}
    private val api = MainApp.api

    fun initData(token: String, errors: List<CorrectionRow>, mappedFields: List<DbField>, onCompleted: () -> Unit) {
        this.token = token
        this.onAllCompletedCallback = onCompleted

        buildTable(mappedFields)
        correctionTable.items = FXCollections.observableArrayList(errors)
        statusLabel.text = "Ошибок найдено: ${errors.size}"
    }

    private fun buildTable(mappedFields: List<DbField>) {
        correctionTable.columns.clear()

        // Колонка с номером строки из Excel
        val rowCol = TableColumn<CorrectionRow, String>("Строка")
        rowCol.setCellValueFactory { SimpleStringProperty(it.value.originalRowIndex.toString()) }
        rowCol.prefWidth = 60.0
        correctionTable.columns.add(rowCol)

        // Колонка с текстом ошибки
        val errCol = TableColumn<CorrectionRow, String>("Ошибка")
        errCol.setCellValueFactory { SimpleStringProperty(it.value.errorMsg) }
        errCol.style = "-fx-text-fill: red; -fx-font-weight: bold;"
        errCol.prefWidth = 250.0
        correctionTable.columns.add(errCol)

        // Динамические колонки для тех полей, которые участвовали в маппинге
        for (field in mappedFields) {
            val col = TableColumn<CorrectionRow, String>(field.title)
            col.setCellValueFactory { SimpleStringProperty(it.value.values[field] ?: "") }
            
            // Делаем ячейки редактируемыми
            col.cellFactory = TextFieldTableCell.forTableColumn()
            col.setOnEditCommit { event ->
                val row = event.tableView.items[event.tablePosition.row]
                row.values[field] = event.newValue // Сохраняем изменения в Map
            }
            col.prefWidth = 100.0
            correctionTable.columns.add(col)
        }
    }

    @FXML
    fun onRetry() {
        val stillInvalid = mutableListOf<CorrectionRow>()
        val validWorkings = mutableListOf<Working>()

        // Снова прогоняем все строки из таблицы через парсер
        for (row in correctionTable.items) {
            try {
                val working = WorkingParser.parse(row.values)
                validWorkings.add(working)
            } catch (e: Exception) {
                row.errorMsg = e.message ?: "Ошибка формата"
                stillInvalid.add(row)
            }
        }

        retryButton.isDisable = true
        statusLabel.text = "Отправка исправленных данных..."

        runOnFx {
            try {
                if (validWorkings.isNotEmpty()) {
                    api.createWorkingsBatch("Bearer $token", validWorkings).await()
                }

                // Если ошибки всё еще остались — обновляем таблицу
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

    @FXML
    fun onCancel() {
        // Просто закрываем окно, оставшиеся ошибки проигнорируются
        onAllCompletedCallback()
        close()
    }

    private fun close() {
        (correctionTable.scene.window as Stage).close()
    }
}