package org.example.geoapp.controller

import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.Working
import org.example.geoapp.util.await
import org.example.geoapp.util.toBearerAuthorization
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RecycleBinController {
    private lateinit var api: GeoApi
    private lateinit var token: String

    @FXML
    private lateinit var recycleBinTable: TableView<Working>

    @FXML
    private lateinit var colId: TableColumn<Working, String>

    @FXML
    private lateinit var colNumber: TableColumn<Working, String>

    @FXML
    private lateinit var colArea: TableColumn<Working, String>

    @FXML
    private lateinit var colWorkType: TableColumn<Working, String>

    @FXML
    private lateinit var restoreButton: Button

    fun initData(api: GeoApi, token: String) {
        this.api = api
        this.token = token
        setupColumns()
        loadDeletedWorkings()
    }

    @FXML
    fun initialize() {
    }

    @FXML
    fun close() {
        val stage = recycleBinTable.scene.window as Stage
        stage.close()
    }

    private fun setupColumns() {
        colId.setCellValueFactory { SimpleStringProperty(it.value.id.toString()) }
        colNumber.setCellValueFactory { SimpleStringProperty(it.value.number) }
        colArea.setCellValueFactory { SimpleStringProperty(it.value.area?.name ?: "-") }
        colWorkType.setCellValueFactory { SimpleStringProperty(it.value.workType?.name ?: "-") }

        restoreButton.setOnAction {
            val selected = recycleBinTable.selectionModel.selectedItem
            if (selected != null) {
                restoreWorking(selected.id)
            } else {
                Alert(Alert.AlertType.WARNING, "Выберите запись для восстановления").showAndWait()
            }
        }
    }

    private fun loadDeletedWorkings() {
        GlobalScope.launch {
            try {
                val response = api.getRecycleBin(token.toBearerAuthorization()).await()
                Platform.runLater {
                    recycleBinTable.items = FXCollections.observableArrayList(response)
                }
            } catch (e: Exception) {
                Platform.runLater {
                    Alert(Alert.AlertType.ERROR, "Ошибка при загрузке удалённых записей: ${e.message}").showAndWait()
                }
            }
        }
    }

    private fun restoreWorking(id: Long) {
        GlobalScope.launch {
            try {
                val response = api.restoreWorking(token.toBearerAuthorization(), id).await()
                Platform.runLater {
                    Alert(Alert.AlertType.INFORMATION, "Запись восстановлена: ${response.number}").showAndWait()
                    loadDeletedWorkings()
                }
            } catch (e: Exception) {
                Platform.runLater {
                    Alert(Alert.AlertType.ERROR, "Ошибка при восстановлении: ${e.message}").showAndWait()
                }
            }
        }
    }
}
