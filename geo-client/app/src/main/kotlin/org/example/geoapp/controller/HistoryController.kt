package org.example.geoapp.controller

import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.WorkingAuditEntry
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import java.time.Instant
import java.time.ZoneId
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import retrofit2.HttpException

class HistoryController {

    @FXML private lateinit var historyTable: TableView<WorkingAuditEntry>
    @FXML private lateinit var colWorkingId: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var colRevisionType: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var colTimestamp: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var colUsername: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var colWorkingName: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var colChanges: TableColumn<WorkingAuditEntry, String>
    @FXML private lateinit var restoreButton: Button

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())
    private lateinit var api: GeoApi
    private lateinit var token: String

    fun initData(api: GeoApi, token: String) {
        this.api = api
        this.token = token
        setupColumns()
        loadHistory()
    }

    @FXML fun initialize() {
        historyTable.setRowFactory { _ ->
            val row = TableRow<WorkingAuditEntry>()
            row.setOnMouseClicked { event ->
                if (event.clickCount == 2 && !row.isEmpty) {
                    showChanges(row.item)
                }
            }
            row
        }
    }

    @FXML fun close() {
        (historyTable.scene.window as Stage).close()
    }

    private fun setupColumns() {
        colWorkingId.setCellValueFactory { SimpleStringProperty(it.value.workingId.toString()) }
        colRevisionType.setCellValueFactory { SimpleStringProperty(it.value.revisionType) }
        colTimestamp.setCellValueFactory { colTimestamp ->
            val rawDate = colTimestamp.value.revisionTimestamp
            val formattedDate = try {
                    val instant = OffsetDateTime.parse(rawDate).toInstant()
                    dateFormatter.format(instant)
            } catch (e: Exception) {
                rawDate?.toString() ?: ""
            }
            SimpleStringProperty(formattedDate)
        }
        colUsername.setCellValueFactory { SimpleStringProperty(it.value.username ?: "Система") }
        colWorkingName.setCellValueFactory { SimpleStringProperty(it.value.details.number ?: "") }
        //colChanges.setCellValueFactory { SimpleStringProperty(formatChanges(it.value)) }
        colChanges.setCellValueFactory { SimpleStringProperty(it.value.changesText) }
        // colChanges.setCellFactory { _ ->
        //     object : TableCell<WorkingAuditEntry, String>() {
        //         override fun updateItem(item: String?, empty: Boolean) {
        //             super.updateItem(item, empty)
        //             text = if (empty || item == null) "" else item
        //             if (!empty && item != null) {
        //                 val entry = tableRow?.item
        //                 if (entry != null && entry.changedFields.isNotEmpty()) {
        //                     val fullText = entry.changedFields.joinToString("\n") { change ->
        //                         "${change.field}: ${change.oldValue ?: "∅"} → ${change.newValue ?: "∅"}"
        //                     }
        //                     tooltip = Tooltip(fullText)
        //                 } else {
        //                     tooltip = null
        //                 }
        //             } else {
        //                 tooltip = null
        //             }
        //         }
        //     }
        // }
    }

    private fun autoResizeColumns() {
        fun resizeColumn(col: TableColumn<*, *>) {
            // Ширина заголовка + отступ
            var maxWidth = javafx.scene.text.Text(col.text).layoutBounds.width + 25.0
            // Проверяем данные в ячейках (до 50 строк для производительности)
            for (i in 0 until minOf(historyTable.items.size, 50)) {
                val cellData = col.getCellData(i)?.toString() ?: ""
                val width = javafx.scene.text.Text(cellData).layoutBounds.width + 15.0
                if (width > maxWidth) maxWidth = width
            }
            col.prefWidth = maxWidth
            // Рекурсивно обрабатываем вложенные колонки
            col.columns.forEach { resizeColumn(it) }
        }
        historyTable.columns.forEach { resizeColumn(it) }
    }

    private fun loadHistory() {
        runOnFx {
            try {
                val auditList = api.getWorkingAuditHistory("Bearer $token").await()
                val sortedList = auditList.sortedByDescending { it.revisionNumber }
                historyTable.items = FXCollections.observableArrayList(sortedList)
                autoResizeColumns()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Ошибка загрузки истории: ${e.message}").showAndWait()
            }
        }
    }

    private fun showChanges(entry: WorkingAuditEntry?) {
        if (entry == null) return
        
        val text = entry.changesText
        
        if (text.isBlank() || text == "Без изменений") {
            Alert(Alert.AlertType.INFORMATION, "Нет изменений для этой ревизии").showAndWait()
            return
        }
        
        // Создаем окно с прокруткой, если изменений очень много
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Детали ревизии"
        alert.headerText = "Изменения выработки ${entry.details.number ?: ""}"
        
        val textArea = TextArea(text).apply {
            isEditable = false
            isWrapText = true
            prefWidth = 400.0
            prefHeight = 300.0
        }
        
        alert.dialogPane.content = textArea
        alert.showAndWait()
    }

    @FXML fun restoreRevision() {
        val selected = historyTable.selectionModel.selectedItem ?: return
        runOnFx {
            try {
                // Сначала проверяем, нет ли конфликта номера
                val restored = api.restoreRevision("Bearer $token", selected.workingId, selected.revisionNumber).await()
                //Alert(Alert.AlertType.INFORMATION, "Ревизия ${selected.revisionNumber} восстановлена. Номер: ${restored.number}").showAndWait()
                loadHistory()
                // Можно также обновить основную таблицу (но MainController сам перезагрузит при закрытии окна)
            } catch (e: Exception) {
                Alert(Alert.AlertType.INFORMATION,"Доделать").showAndWait()
                if (e.message?.contains("HTTP 409") == true) {
                    val confirm = Alert( Alert.AlertType.CONFIRMATION, "Выработка с таким номером уже существует. Перезаписать?", ButtonType.YES, ButtonType.NO)
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        Alert(Alert.AlertType.INFORMATION,"Функция принудительного восстановления пока не реализована").showAndWait()
                    }
                    return@runOnFx
                }

                Alert(Alert.AlertType.ERROR, "Ошибка восстановления: ${e.message}").showAndWait()
            }
        }
    }


}
