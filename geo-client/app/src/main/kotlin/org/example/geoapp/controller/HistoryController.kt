package org.example.geoapp.controller

import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.WorkingAuditEntry
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.beans.property.SimpleStringProperty
import javafx.stage.Stage

class HistoryController {
    private lateinit var api: GeoApi
    private lateinit var token: String

    @FXML
    private lateinit var historyTable: TableView<WorkingAuditEntry>

    @FXML
    private lateinit var colWorkingId: TableColumn<WorkingAuditEntry, String>

    @FXML
    private lateinit var colRevisionType: TableColumn<WorkingAuditEntry, String>

    @FXML
    private lateinit var colTimestamp: TableColumn<WorkingAuditEntry, String>

    @FXML
    private lateinit var colUsername: TableColumn<WorkingAuditEntry, String>

    @FXML
    private lateinit var colWorkingName: TableColumn<WorkingAuditEntry, String>

    fun initData(api: GeoApi, token: String) {
        this.api = api
        this.token = token
        setupColumns()
        loadAuditHistory()
    }

    @FXML
    fun initialize() {
    }

    @FXML
    fun close() {
        val stage = historyTable.scene.window as Stage
        stage.close()
    }

    private fun setupColumns() {
        colWorkingId.setCellValueFactory { SimpleStringProperty(it.value.workingId.toString()) }
        colRevisionType.setCellValueFactory { SimpleStringProperty(it.value.revisionType) }
        colTimestamp.setCellValueFactory { SimpleStringProperty(it.value.revisionTimestamp) }
        colUsername.setCellValueFactory { SimpleStringProperty(it.value.username ?: "System") }
        colWorkingName.setCellValueFactory { SimpleStringProperty(it.value.details.number) }
    }

    private fun authHeader(): String = if (token.startsWith("Bearer ")) token else "Bearer $token"

    private fun loadAuditHistory() {
        runOnFx {
            try {
                val response = api.getWorkingAuditHistory(authHeader()).await()
                historyTable.items = FXCollections.observableArrayList(response)
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Ошибка при загрузке истории: ${e.message}").showAndWait()
            }
        }
    }
}
