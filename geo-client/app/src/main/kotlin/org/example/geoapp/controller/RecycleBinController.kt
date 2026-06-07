package org.example.geoapp.controller

import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.Working
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage

class RecycleBinController {

    @FXML private lateinit var recycleBinTable: TableView<Working>
    @FXML private lateinit var colId: TableColumn<Working, String>
    @FXML private lateinit var colNumber: TableColumn<Working, String>
    @FXML private lateinit var colArea: TableColumn<Working, String>
    @FXML private lateinit var colWorkType: TableColumn<Working, String>
    @FXML private lateinit var restoreButton: Button

    private lateinit var api: GeoApi
    private lateinit var token: String

    fun initData(api: GeoApi, token: String) {
        this.api = api
        this.token = token
        setupColumns()
        loadDeletedWorkings()
    }

    @FXML fun initialize() {}

    @FXML fun close() {
        (recycleBinTable.scene.window as Stage).close()
    }

    private fun setupColumns() {
        colId.setCellValueFactory { SimpleStringProperty(it.value.id.toString()) }
        colNumber.setCellValueFactory { SimpleStringProperty(it.value.number) }
        colArea.setCellValueFactory { SimpleStringProperty(it.value.area?.name ?: "") }
        colWorkType.setCellValueFactory { SimpleStringProperty(it.value.workType?.name ?: "") }
    }

    @FXML fun onRestore() {
        val selected = recycleBinTable.selectionModel.selectedItem ?: return
        runOnFx {
            try {
                api.restoreWorking("Bearer $token", selected.id).await()
                loadDeletedWorkings()
                Alert(Alert.AlertType.INFORMATION, "Запись восстановлена").showAndWait()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Ошибка восстановления: ${e.message}").showAndWait()
            }
        }
    }

    private fun loadDeletedWorkings() {
        runOnFx {
            try {
                val deleted = api.getRecycleBin("Bearer $token").await()
                recycleBinTable.items = FXCollections.observableArrayList(deleted)
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Не удалось загрузить удалённые записи: ${e.message}").showAndWait()
            }
        }
    }
}