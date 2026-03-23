package org.example.geoapp.controller

import com.example.geoapp.api.GeoApi
import com.example.geoapp.api.Working
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import org.controlsfx.control.table.TableFilter
import org.example.geoapp.MainApp
import org.example.geoapp.util.FilterParser
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitVoid
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx

class MainController {

    @FXML private lateinit var adminMenuBar: MenuBar
    @FXML private lateinit var workingsTable: TableView<Working>

    @FXML private lateinit var colRowNumber: TableColumn<Working, Number>
    @FXML private lateinit var colName: TableColumn<Working, String>
    @FXML private lateinit var colArea: TableColumn<Working, String>
    @FXML private lateinit var colWorkType: TableColumn<Working, String> 
    @FXML private lateinit var colDepth: TableColumn<Working, Double?>
    @FXML private lateinit var colGeologist: TableColumn<Working, String>
    @FXML private lateinit var colContractor: TableColumn<Working, String>
    @FXML private lateinit var colPlannedX: TableColumn<Working, Double?>
    @FXML private lateinit var colPlannedY: TableColumn<Working, Double?>
    @FXML private lateinit var colPlannedZ: TableColumn<Working, Double?>
    @FXML private lateinit var colActualX: TableColumn<Working, Double?>
    @FXML private lateinit var colActualY: TableColumn<Working, Double?>
    @FXML private lateinit var colActualZ: TableColumn<Working, Double?>
    @FXML private lateinit var colDeltaX: TableColumn<Working, Double?>
    @FXML private lateinit var colDeltaY: TableColumn<Working, Double?>
    @FXML private lateinit var colCoreRecovery: TableColumn<Working, Double?>
    @FXML private lateinit var colCasing: TableColumn<Working, String?>
    @FXML private lateinit var colClosureStage: TableColumn<Working, String?>
    @FXML private lateinit var colStartDate: TableColumn<Working, String>
    @FXML private lateinit var colEndDate: TableColumn<Working, String>
    @FXML private lateinit var colAdditionalInfo: TableColumn<Working, String>
    @FXML private lateinit var colMmg1Top: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg1Bottom: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg2Top: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg2Bottom: TableColumn<Working, Double?>
    @FXML private lateinit var colGwAppearLog: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableLog: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableAbs: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableRel: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableAbsFinal: TableColumn<Working, Double?>
    @FXML private lateinit var colContractorExtraIndex: TableColumn<Working, String?>
    @FXML private lateinit var colAct: TableColumn<Working, String?>
    @FXML private lateinit var colActNumber: TableColumn<Working, String?>
    @FXML private lateinit var colThermalTube: TableColumn<Working, String?>

    @FXML private lateinit var addButton: Button
    @FXML private lateinit var editButton: Button
    @FXML private lateinit var deleteButton: Button

    private lateinit var token: String
    private lateinit var userRole: String
    private val api: GeoApi = MainApp.api
    private val workingsList: ObservableList<Working> = FXCollections.observableArrayList()

    private lateinit var tableFilter: TableFilter<Working>

    fun initData(token: String, role: String) {
        this.token = token
        this.userRole = role
        
        // Показываем меню только если роль "ROLE_ADMIN"
        adminMenuBar.isVisible = (role == "ROLE_ADMIN")
        loadWorkings()
    }

    @FXML fun initialize() {
        // Существующие колонки
        colRowNumber.setCellValueFactory { cellData ->
            val index = workingsTable.items.indexOf(cellData.value) + 1
            javafx.beans.property.SimpleIntegerProperty(index)
        }
        colName.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.number) }
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
        colMmg1Top.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.mmg1Top) }
        colMmg1Bottom.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.mmg1Bottom) }
        colMmg2Top.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.mmg2Top) }
        colMmg2Bottom.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.mmg2Bottom) }
        colGwAppearLog.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.gwAppearLog) }
        colGwStableLog.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.gwStableLog) }
        colGwStableAbs.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.gwStableAbs) }
        colGwStableRel.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.gwStableRel) }
        colGwStableAbsFinal.setCellValueFactory { cellData -> javafx.beans.property.SimpleObjectProperty(cellData.value.gwStableAbsFinal) }
        colContractorExtraIndex.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.contractorExtraIndex) }
        colAct.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.act) }
        colActNumber.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.actNumber) }
        colThermalTube.setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value.thermalTube) }

        workingsTable.items = workingsList

        // Кнопки
        editButton.disableProperty().bind(workingsTable.selectionModel.selectedItemProperty().isNull())
        deleteButton.disableProperty().bind(workingsTable.selectionModel.selectedItemProperty().isNull())

        // Горячие клавиши
        // Двойной клик
        workingsTable.setOnMouseClicked { event ->
            if (event.clickCount == 2 && !workingsTable.selectionModel.selectedItems.isEmpty()) {
                onEdit()
            }
        }
        // Ctrl+E
        workingsTable.sceneProperty().addListener { _, _, newScene ->
            if (newScene != null) {
                newScene.accelerators[KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN)] = Runnable {
                    if (!editButton.isDisable) onEdit()
                }
            }
        }
    }

    fun loadWorkings() {
        runOnFx {
            try {
                val workings = api.getWorkings("Bearer $token").await()
                    .sortedBy { it.id }
                workingsList.setAll(workings)

                if (!::tableFilter.isInitialized) {
                    tableFilter = TableFilter
                        .forTableView(workingsTable)
                        .lazy(false)
                        .apply()

                    tableFilter.setSearchStrategy { input: String, target: String ->
                        FilterParser.parse(input, target)
                    }
                }
            } catch (e: Exception) {
                showAlert("Ошибка загрузки", "Не удалось загрузить список выработок: ${e.message}")
            }
        }
    }

    @FXML fun onAdd() {
        showWorkingForm(null)
    }

    @FXML fun onEdit() {
        val selected = workingsTable.selectionModel.selectedItem
        if (selected != null) {
            showWorkingForm(selected)
        }
    }

    @FXML fun onDelete() {
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
                        api.deleteWorking("Bearer $token", selected.id).awaitUnit()
                        loadWorkings()
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

    @FXML fun openAreasEditor() = openRefEditor(RefType.AREA)
    @FXML fun openWorkTypesEditor() = openRefEditor(RefType.WORK_TYPE)
    @FXML fun openDrillingRigsEditor() = openRefEditor(RefType.DRILLING_RIG)
    @FXML fun openContractorsEditor() = openRefEditor(RefType.CONTRACTOR)
    @FXML fun openGeologistsEditor() = openRefEditor(RefType.GEOLOGIST)

    private fun openRefEditor(type: RefType) {
        val loader = FXMLLoader(javaClass.getResource("/referenceEditor.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ReferenceEditorController>()
        controller.initData(token, type)

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Справочник: ${type.title}"
        stage.showAndWait()
        
        // После закрытия справочников перезагружаем главную таблицу (вдруг имена изменились)
        loadWorkings() 
    }

    @FXML
    fun openExcelImport() {
        val fileChooser = FileChooser()
        fileChooser.title = "Выберите Excel файл для импорта"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx", "*.xlsm"))
        
        val file = fileChooser.showOpenDialog(workingsTable.scene.window)
        
        if (file != null) {
            try {
                val loader = FXMLLoader(javaClass.getResource("/excelImport.fxml"))
                val root = loader.load<VBox>()
                val controller = loader.getController<ExcelImportController>()
                
                controller.initData(token, file) { loadWorkings() }

                val stage = Stage()
                stage.initModality(Modality.WINDOW_MODAL)
                stage.initOwner(workingsTable.scene.window)
                stage.scene = Scene(root)
                stage.title = "Мастер импорта выработок"
                stage.showAndWait()
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось открыть окно импорта: ${e.message}")
            }
        }
    }
    
}