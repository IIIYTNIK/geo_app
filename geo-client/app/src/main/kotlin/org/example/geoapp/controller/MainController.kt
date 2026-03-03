package org.example.geoapp.controller

import com.example.geoapp.api.GeoApi
import com.example.geoapp.api.Working
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitVoid
import org.example.geoapp.util.AutoFilterRow
import javafx.scene.Scene

class MainController {

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var workingsTable: TableView<Working>

    @FXML
    private lateinit var filterRow: HBox  // Добавлено

    @FXML
    private lateinit var filterScrollPane: ScrollPane

    // Колонки таблицы
    @FXML
    private lateinit var colRowNumber: TableColumn<Working, Number>
    @FXML
    private lateinit var colName: TableColumn<Working, String>
    @FXML
    private lateinit var colArea: TableColumn<Working, String>
    @FXML
    private lateinit var colWorkType: TableColumn<Working, String>
    @FXML
    private lateinit var colDepth: TableColumn<Working, Double?>
    @FXML
    private lateinit var colGeologist: TableColumn<Working, String>
    @FXML
    private lateinit var colContractor: TableColumn<Working, String>
    @FXML
    private lateinit var colPlannedX: TableColumn<Working, Double?>
    @FXML
    private lateinit var colPlannedY: TableColumn<Working, Double?>
    @FXML
    private lateinit var colPlannedZ: TableColumn<Working, Double?>
    @FXML
    private lateinit var colActualX: TableColumn<Working, Double?>
    @FXML
    private lateinit var colActualY: TableColumn<Working, Double?>
    @FXML
    private lateinit var colActualZ: TableColumn<Working, Double?>
    @FXML
    private lateinit var colDeltaX: TableColumn<Working, Double?>
    @FXML
    private lateinit var colDeltaY: TableColumn<Working, Double?>
    @FXML
    private lateinit var colCoreRecovery: TableColumn<Working, Double?>
    @FXML
    private lateinit var colCasing: TableColumn<Working, String?>
    @FXML
    private lateinit var colClosureStage: TableColumn<Working, String?>

    @FXML
    private lateinit var colStartDate: TableColumn<Working, String>
    @FXML
    private lateinit var colEndDate: TableColumn<Working, String>
    @FXML
    private lateinit var colAdditionalInfo: TableColumn<Working, String>
    @FXML
    private lateinit var addButton: Button
    @FXML
    private lateinit var editButton: Button
    @FXML
    private lateinit var deleteButton: Button

    private lateinit var token: String
    private val api: GeoApi = MainApp.api
    private val workingsList: ObservableList<Working> = FXCollections.observableArrayList()
    private val filteredList = FilteredList(workingsList)

    private lateinit var autoFilterRow: AutoFilterRow<Working>

    fun setToken(token: String) {
        this.token = token
        loadWorkings()
    }

    @FXML
    fun initialize() {
        colRowNumber.setCellValueFactory { cellData ->
            val index = workingsTable.items.indexOf(cellData.value) + 1
            javafx.beans.property.SimpleIntegerProperty(index)
        }
        colName.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.number ?: "") }
        colArea.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.area?.name ?: "") }
        colWorkType.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.workType?.name ?: "") }
        colDepth.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.depth) }
        colGeologist.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.geologist?.name ?: "") }
        colContractor.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.contractor?.name ?: "") }
        colPlannedX.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.plannedX) }
        colPlannedY.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.plannedY) }
        colPlannedZ.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.plannedZ) }
        colActualX.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.actualX) }
        colActualY.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.actualY) }
        colActualZ.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.actualZ) }
        colDeltaX.setCellValueFactory { cellData ->
            val dx = cellData.value.actualX?.minus(cellData.value.plannedX ?: 0.0)
            javafx.beans.property.SimpleObjectProperty(dx)
        }
        colDeltaY.setCellValueFactory { cellData ->
            val dy = cellData.value.actualY?.minus(cellData.value.plannedY ?: 0.0)
            javafx.beans.property.SimpleObjectProperty(dy)
        }
        colCoreRecovery.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.coreRecovery) }
        colCasing.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.casing ?: "") }
        colClosureStage.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.closureStage ?: "") }
        colStartDate.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.startDate ?: "") }
        colEndDate.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.endDate ?: "") }
        colAdditionalInfo.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.additionalInfo ?: "") }

        workingsTable.items = filteredList

        // Создаём AutoFilterRow и добавляем его в filterRow
        autoFilterRow = AutoFilterRow(workingsTable, filteredList) { }
        filterRow.children.add(autoFilterRow.node)
        
        filterRow.prefWidthProperty().bind(workingsTable.widthProperty())

        // Синхронизация горизонтальной прокрутки
        Platform.runLater {
            val tableScrollBar = findHorizontalScrollBar(workingsTable)
            val filterScrollBar = filterScrollPane.hvalueProperty()

            if (tableScrollBar != null) {
                // При прокрутке таблицы двигаем ScrollPane фильтра
                tableScrollBar.valueProperty().addListener { _, _, newValue ->
                    filterScrollPane.hvalue = newValue.toDouble()
                }
                // При прокрутке фильтра двигаем таблицу (двусторонняя синхронизация)
                filterScrollBar.addListener { _, _, newValue ->
                    tableScrollBar.value = newValue.toDouble()
                }
            }
        }

        // Поиск (если используется)
        searchField.textProperty().addListener { _, _, _ ->
            autoFilterRow.applyFilters()
        }
    }

    
    // Вспомогательная функция для поиска горизонтального ScrollBar в TableView
    private fun findHorizontalScrollBar(tableView: TableView<*>): ScrollBar? {
        return tableView.lookupAll(".scroll-bar").firstOrNull {
            it is ScrollBar && it.orientation == Orientation.HORIZONTAL
        } as? ScrollBar
    }

    
    fun loadWorkings() {
        runOnFx {
            try {
                val workings = api.getWorkings("Bearer $token").await()
                workingsList.setAll(workings)
                autoFilterRow.applyFilters()  
            } catch (e: Exception) {
                showAlert("Ошибка загрузки", "Не удалось загрузить список выработок: ${e.message}")
            }
        }
    }


    @FXML
    fun onAdd() {
        showWorkingForm(null)
    }

    @FXML
    fun onEdit() {
        val selected = workingsTable.selectionModel.selectedItem
        if (selected != null) {
            showWorkingForm(selected)
        }
    }

    @FXML
    fun onDelete() {
        val selected = workingsTable.selectionModel.selectedItem
        if (selected != null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Подтверждение"
            alert.headerText = "Удалить выработку ${selected.number}?"
            alert.contentText = "Это действие нельзя отменить."
            val result = alert.showAndWait()
            if (result.isPresent && result.get() == ButtonType.OK) {
                runOnFx {
                    try {
                        api.deleteWorking("Bearer $token", selected.id).awaitVoid()
                        loadWorkings() // обновить список
                    } catch (e: Exception) {
                        showAlert("Ошибка удаления", e.message ?: "Неизвестная ошибка")
                    }
                }
            }
        }
    }

    private fun showWorkingForm(working: Working?) {
        val loader = FXMLLoader(javaClass.getResource("/workingForm.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<WorkingFormController>()
        controller.initData(token, working, this::loadWorkings)

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = if (working == null) "Новая выработка" else "Редактирование"
        stage.showAndWait()
    }

    private fun showAlert(title: String, message: String) {
        Alert(Alert.AlertType.ERROR).apply {
            this.title = title
            headerText = null
            contentText = message
            showAndWait()
        }
    }
}