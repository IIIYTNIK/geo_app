package org.example.geoapp.controller

import org.example.geoapp.api.UserDto
import org.example.geoapp.api.UserAreaAccessDto
import org.example.geoapp.api.AccessLevel
import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.Working
import org.example.geoapp.api.RefContractor
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.input.ContextMenuEvent
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.VBox
import javafx.scene.layout.Pane
import javafx.scene.layout.HBox
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.control.TableColumn.SortType
import javafx.scene.shape.Rectangle
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.util.Duration
import javafx.util.converter.IntegerStringConverter
import javafx.util.converter.DoubleStringConverter
import javafx.util.StringConverter
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.application.Platform
import javafx.geometry.Bounds
import javafx.geometry.Orientation
import org.controlsfx.control.table.TableFilter
import org.example.geoapp.MainApp
import org.example.geoapp.util.FilterParser
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx
import org.example.geoapp.controller.UserListController
import org.example.geoapp.controller.HistoryController
import org.example.geoapp.controller.RecycleBinController


class MainController {

    @FXML private lateinit var summaryContainer: Pane
    @FXML private lateinit var summaryHBox: HBox
    
    @FXML private lateinit var adminMenu: Menu
    @FXML private lateinit var filterAreaCombo: ComboBox<String>
    @FXML private lateinit var workingsTable: TableView<Working>

    @FXML private lateinit var colRowNumber: TableColumn<Working, Number>
    @FXML private lateinit var colName: TableColumn<Working, String>
    @FXML private lateinit var colWorkType: TableColumn<Working, String>
    @FXML private lateinit var colArea: TableColumn<Working, String>
    @FXML private lateinit var colGeologist: TableColumn<Working, String>
    @FXML private lateinit var colContractor: TableColumn<Working, String>
    @FXML private lateinit var colDrillingRig: TableColumn<Working, String>
    @FXML private lateinit var colStructure: TableColumn<Working, String>
    @FXML private lateinit var colPlannedContractor: TableColumn<Working, String>
    @FXML private lateinit var colPlannedX: TableColumn<Working, Double?>
    @FXML private lateinit var colPlannedY: TableColumn<Working, Double?>
    @FXML private lateinit var colPlannedDepth: TableColumn<Working, Double?>
    @FXML private lateinit var colActualX: TableColumn<Working, Double?>
    @FXML private lateinit var colActualY: TableColumn<Working, Double?>
    @FXML private lateinit var colActualZ: TableColumn<Working, Double?>
    @FXML private lateinit var colActualDepth: TableColumn<Working, Double?>
    @FXML private lateinit var colDeltaS: TableColumn<Working, Double?>
    @FXML private lateinit var colCoreRecovery: TableColumn<Working, Double?>
    @FXML private lateinit var colCasing: TableColumn<Working, Double?>
    
    // Чекбоксы
    @FXML private lateinit var colHasVideo: TableColumn<Working, Boolean>
    @FXML private lateinit var colHasDrilling: TableColumn<Working, Boolean>
    @FXML private lateinit var colHasJournal: TableColumn<Working, Boolean>
    @FXML private lateinit var colHasCore: TableColumn<Working, Boolean>
    @FXML private lateinit var colHasStake: TableColumn<Working, Boolean>

    @FXML private lateinit var colEmergency: TableColumn<Working, Boolean>

    // Образцы
    @FXML private lateinit var colSamplesThawed: TableColumn<Working, Int?>
    @FXML private lateinit var colSamplesFrozen: TableColumn<Working, Int?>
    @FXML private lateinit var colSamplesRocky: TableColumn<Working, Int?>

    @FXML private lateinit var colStartDate: TableColumn<Working, String>
    @FXML private lateinit var colEndDate: TableColumn<Working, String>
    @FXML private lateinit var colMmg1Top: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg1Bottom: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg2Top: TableColumn<Working, Double?>
    @FXML private lateinit var colMmg2Bottom: TableColumn<Working, Double?>
    @FXML private lateinit var colGwAppearLog: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableLog: TableColumn<Working, Double?>
    @FXML private lateinit var colGwStableAbs: TableColumn<Working, Double?>
    @FXML private lateinit var colAct: TableColumn<Working, String>
    @FXML private lateinit var colActNumber: TableColumn<Working, String?>
    @FXML private lateinit var colThermalTube: TableColumn<Working, String>
    @FXML private lateinit var colAdditionalInfo: TableColumn<Working, String>

    @FXML private lateinit var addButton: Button
    @FXML private lateinit var editButton: Button
    @FXML private lateinit var deleteButton: Button

    @FXML private lateinit var colCat1_4: TableColumn<Working, Double?>
    @FXML private lateinit var colCat5_8: TableColumn<Working, Double?>
    @FXML private lateinit var colCat9_12: TableColumn<Working, Double?>

    private var autoRefreshTimeline: Timeline? = null
    private lateinit var currentUser: UserDto
    lateinit var token: String
    private lateinit var userRole: String
    private var userAccessByAreaId: Map<Long, AccessLevel> = emptyMap()
    private lateinit var rowContextMenu: ContextMenu
    private val api: GeoApi = MainApp.api
    private val allWorkings: MutableList<Working> = mutableListOf()
    private val workingsList: ObservableList<Working> = FXCollections.observableArrayList()

    private lateinit var tableFilter: TableFilter<Working>

    private var tableFilterRebuildScheduled = false
    private var tableFilterItemsListener: ListChangeListener<Working>? = null

    // Хранилище ярлыков (Label) для каждой колонки
    private val summaryLabels = mutableMapOf<TableColumn<Working, *>, Label>()

    private var summaryScrollBar: ScrollBar? = null
    private var summaryScrollListener: ChangeListener<Number>? = null

    fun initData(token: String, role: String, user: UserDto) {
        this.token = token
        this.userRole = role
        this.currentUser = user
        adminMenu.isVisible = (role == "ROLE_ADMIN")
        updateAccessControls()
        loadUserAccess()
        loadWorkings()
        startAutoRefresh()
    }

    private fun authHeader(): String = if (token.startsWith("Bearer ")) token else "Bearer $token"

    // private fun setupTableNavigation() {
    //     workingsTable.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
    //         if (event.code == KeyCode.ENTER) {
    //             // Если мы сейчас находимся в режиме редактирования
    //             val pos = workingsTable.editingCell
    //             if (pos != null) {
    //                 // Вычисляем следующую строку (вниз)
    //                 val nextRow = pos.row + 1
    //                 val column = pos.tableColumn
                    
    //                 // Чтобы переход сработал корректно, нужно дождаться завершения текущего цикла событий
    //                 Platform.runLater {
    //                     if (nextRow < workingsTable.items.size) {
    //                         // Выделяем и переходим в режим редактирования следующей ячейки
    //                         workingsTable.selectionModel.clearAndSelect(nextRow, column)
    //                         workingsTable.edit(nextRow, column)
    //                     }
    //                 }
    //             } else {
    //                 // Если мы НЕ в режиме редактирования, просто начинаем его для выбранной ячейки
    //                 val selectedPos = workingsTable.selectionModel.selectedCells.firstOrNull()
    //                 if (selectedPos != null) {
    //                     workingsTable.edit(selectedPos.row, selectedPos.tableColumn)
    //                     event.consume() // Предотвращаем стандартный переход фокуса
    //                 }
    //             }
    //         }
    //     }
    // }

    @FXML fun initialize() {
        workingsTable.isEditable = true
        workingsTable.selectionModel.selectionMode = SelectionMode.MULTIPLE

        val selectionModel = workingsTable.selectionModel
        selectionModel.selectionMode = SelectionMode.MULTIPLE

        // 
        workingsTable.focusModel.focusedCellProperty().addListener { _, oldPos, newPos ->
            // Снимаем класс подсветки с предыдущей колонки
            oldPos?.tableColumn?.styleClass?.remove("crosshair-column")
            
            // Вешаем класс подсветки на новую колонку
            newPos?.tableColumn?.let { col ->
                if (!col.styleClass.contains("crosshair-column")) {
                    col.styleClass.add("crosshair-column")
                }
            }
        }


        // Сборка Наименования (Тип + Номер)
        colName.setCellValueFactory { cellData -> 
            val w = cellData.value
            val prefix = when(w.workType?.name?.lowercase()) {
                "скважина" -> "С-"
                "шурф" -> "Ш-"
                "расчистка" -> "Р-"
                else -> ""
            }
            SimpleStringProperty("$prefix${w.number}")
        }

        colRowNumber.isSortable = false

        colRowNumber.setCellValueFactory { 
            javafx.beans.property.ReadOnlyObjectWrapper(0 as Number) 
        }
        
        colRowNumber.setCellFactory {
            object : javafx.scene.control.TableCell<Working, Number>() {
                override fun updateItem(item: Number?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || tableRow == null) null else (tableRow.index + 1).toString()
                }
            }
        }

        //colRowNumber.setCellValueFactory { SimpleObjectProperty(it.value.orderNum) }
        colArea.setCellValueFactory { SimpleStringProperty(it.value.area?.name ?: "") }
        colWorkType.setCellValueFactory { SimpleStringProperty(it.value.workType?.name ?: "") }
        colGeologist.setCellValueFactory { SimpleStringProperty(it.value.geologist?.name ?: "") }
        colContractor.setCellValueFactory { SimpleStringProperty(it.value.contractor?.name ?: "") }
        colDrillingRig.setCellValueFactory { SimpleStringProperty(it.value.drillingRig?.name ?: "") }
        colStructure.setCellValueFactory { SimpleStringProperty(it.value.structure ?: "") }
        colPlannedContractor.setCellValueFactory { SimpleStringProperty(it.value.plannedContractor?.name ?: "") }

        setupDoubleColumn(colPlannedX) { it.plannedX }
        setupDoubleColumn(colPlannedY) { it.plannedY }
        setupDoubleColumn(colPlannedDepth) { it.plannedDepth }
        setupDoubleColumn(colActualX) { it.actualX }
        setupDoubleColumn(colActualY) { it.actualY }
        setupDoubleColumn(colActualZ) { it.actualZ }
        setupDoubleColumn(colActualDepth) { it.actualDepth }
        setupDoubleColumn(colDeltaS) { it.deltaS }
        setupDoubleColumn(colCoreRecovery) { it.coreRecovery }
        setupDoubleColumn(colCasing) { it.casing }
        setupDoubleColumn(colMmg1Top) { it.mmg1Top }
        setupDoubleColumn(colMmg1Bottom) { it.mmg1Bottom }
        setupDoubleColumn(colMmg2Top) { it.mmg2Top }
        setupDoubleColumn(colMmg2Bottom) { it.mmg2Bottom }
        setupDoubleColumn(colGwAppearLog) { it.gwAppearLog }
        setupDoubleColumn(colGwStableLog) { it.gwStableLog }
        setupDoubleColumn(colGwStableAbs) { it.gwStableAbs }
        setupDoubleColumn(colCat1_4) { it.cat1_4 }
        setupDoubleColumn(colCat5_8) { it.cat5_8 }
        setupDoubleColumn(colCat9_12) { it.cat9_12 }

        // Интерактивные чекбоксы в таблице
        createInteractiveCheckbox(colHasVideo, { it.hasVideo }, { w, v -> w.hasVideo = v }, false, true)
        createInteractiveCheckbox(colHasDrilling, { it.hasDrilling }, { w, v -> w.hasDrilling = v }, false, true)
        createInteractiveCheckbox(colHasJournal, { it.hasJournal }, { w, v -> w.hasJournal = v }, false, false)
        createInteractiveCheckbox(colHasCore, { it.hasCore }, { w, v -> w.hasCore = v }, false, false)
        createInteractiveCheckbox(colHasStake, { it.hasStake }, { w, v -> w.hasStake = v }, false, true)
        createInteractiveCheckbox(colEmergency, { it.emergency }, { w, v -> w.emergency = v }, true, false)
        
        colSamplesThawed.setCellValueFactory { SimpleObjectProperty(it.value.samplesThawed) }
        colSamplesFrozen.setCellValueFactory { SimpleObjectProperty(it.value.samplesFrozen) }
        colSamplesRocky.setCellValueFactory { SimpleObjectProperty(it.value.samplesRocky) }

        colStartDate.setCellValueFactory { SimpleStringProperty(it.value.startDate ?: "") }
        colEndDate.setCellValueFactory { SimpleStringProperty(it.value.endDate ?: "") }
        colMmg1Top.setCellValueFactory { SimpleObjectProperty(it.value.mmg1Top) }
        colMmg1Bottom.setCellValueFactory { SimpleObjectProperty(it.value.mmg1Bottom) }
        colMmg2Top.setCellValueFactory { SimpleObjectProperty(it.value.mmg2Top) }
        colMmg2Bottom.setCellValueFactory { SimpleObjectProperty(it.value.mmg2Bottom) }
        colGwAppearLog.setCellValueFactory { SimpleObjectProperty(it.value.gwAppearLog) }
        colGwStableLog.setCellValueFactory { SimpleObjectProperty(it.value.gwStableLog) }
        colGwStableAbs.setCellValueFactory { SimpleObjectProperty(it.value.gwStableAbs) }
        
        colAct.setCellValueFactory { SimpleStringProperty(if (it.value.act) "Да" else "Нет") }
        colActNumber.setCellValueFactory { SimpleStringProperty(it.value.actNumber ?: "") }
        colThermalTube.setCellValueFactory { SimpleStringProperty(if (it.value.thermalTube) "Да" else "Нет") }
        colAdditionalInfo.setCellValueFactory { SimpleStringProperty(it.value.additionalInfo ?: "") }

        workingsTable.items = workingsList

        formatColumn(colPlannedDepth, 1) // План Н
        formatColumn(colActualDepth, 1)  // Факт Н
        formatColumn(colCoreRecovery, 0) // Выход керна
        formatColumn(colCasing, 1)       // Обсад

        formatColumn(colMmg1Top, 1)
        formatColumn(colMmg1Bottom, 1)
        formatColumn(colMmg2Top, 1)
        formatColumn(colMmg2Bottom, 1)
        formatColumn(colGwAppearLog, 1)
        formatColumn(colGwStableLog, 1)

        formatColumn(colCat1_4, 1)
        formatColumn(colCat5_8, 1)
        formatColumn(colCat9_12, 1)
        
        setupEditableIntCol(colSamplesThawed) { w, v -> w.samplesThawed = v }
        setupEditableIntCol(colSamplesFrozen) { w, v -> w.samplesFrozen = v }
        setupEditableIntCol(colSamplesRocky) { w, v -> w.samplesRocky = v }

        setupEditableDoubleColumn(colCat1_4) { w, v -> w.cat1_4 = v }
        setupEditableDoubleColumn(colCat5_8) { w, v -> w.cat5_8 = v }
        setupEditableDoubleColumn(colCat9_12) { w, v -> w.cat9_12 = v }

        // Контекстное меню
        rowContextMenu = ContextMenu()
        val assignStructureItem = MenuItem("Присвоить сооружение").apply { setOnAction { onAssignStructure() } }
        val assignPlannedContractorItem = MenuItem("Назначить подрядчика").apply { setOnAction { onAssignPlannedContractor() } }
        rowContextMenu.items.addAll(assignStructureItem, assignPlannedContractorItem)
        val editItem = MenuItem("Редактировать").apply { setOnAction { onEdit() } }
        val deleteItem = MenuItem("Удалить").apply { setOnAction { onDelete() } }
        rowContextMenu.items.addAll(editItem, deleteItem)

        workingsTable.contextMenu = rowContextMenu
        workingsTable.selectionModel.selectedItemProperty().addListener { _, _, _ -> updateAccessControls() }

        editButton.isDisable = true
        deleteButton.isDisable = true

        workingsTable.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED) { event ->
            val selected = workingsTable.selectionModel.selectedItem
            if (!canWriteArea(selected?.area?.id)) {
                event.consume()
            }
        }

        workingsTable.setOnKeyPressed { event ->
            if (event.code == javafx.scene.input.KeyCode.DELETE) {
                onDelete()
            }
        }

        workingsTable.setOnMouseClicked { event ->
            if (event.clickCount == 2 && !workingsTable.selectionModel.isEmpty) onEdit()
        }

        workingsTable.sceneProperty().addListener { _, _, newScene ->
            newScene?.accelerators?.put(KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), Runnable { if (!editButton.isDisable) onEdit() })
        }

        //Цветовая раскраска строк (Оранжевый / Зеленый)
        workingsTable.setRowFactory {
            object : TableRow<Working>() {
                override fun updateItem(item: Working?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll(listOf("project-filled", "project-empty", "actual", "emergency-row", "read-only-row"))
                    if (item == null || empty) return
                    when {
                        item.emergency -> styleClass.add("emergency-row")
                        isRowReadOnly(item) -> styleClass.add("read-only-row")
                        item.isProject != true -> 
                            styleClass.add("project-filled")
                        item.isProject -> 
                            styleClass.add("project-empty")
                        else -> 
                            styleClass.add("actual")
                    }
                }
            }
        }
        setupSummaryRow()
    }

    private fun loadUserAccess() {
        runOnFx {
            try {
                val accessList = api.getCurrentUserAccess(authHeader()).await()
                userAccessByAreaId = accessList.associate { it.areaId to it.accessLevel }
            } catch (e: Exception) {
                if (userRole != "ROLE_ADMIN") {
                    showAlert("Ошибка доступа", "Не удалось загрузить права текущего пользователя: ${e.message}")
                }
            } finally {
                updateAccessControls()
                workingsTable.refresh()
            }
        }
    }

    private fun getAreaAccessLevel(areaId: Long?): AccessLevel? =
        if (userRole == "ROLE_ADMIN") AccessLevel.WRITE else areaId?.let { userAccessByAreaId[it] }

    private fun canWriteArea(areaId: Long?): Boolean =
        getAreaAccessLevel(areaId) == AccessLevel.WRITE

    private fun canWriteSelectedRow(): Boolean =
        canWriteArea(workingsTable.selectionModel.selectedItem?.area?.id)

    private fun canWriteAnyArea(): Boolean =
        userRole == "ROLE_ADMIN" || userAccessByAreaId.values.any { it == AccessLevel.WRITE }

    private fun canOpenContextMenu(): Boolean =
        canWriteAnyArea() && canWriteSelectedRow()

    private fun isRowReadOnly(item: Working?): Boolean =
        item != null && !canWriteArea(item.area?.id)

    private fun updateAccessControls() {
        addButton.isDisable = !canWriteAnyArea()
        editButton.isDisable = !canWriteSelectedRow()
        deleteButton.isDisable = !canWriteSelectedRow()
        workingsTable.contextMenu = if (canOpenContextMenu()) rowContextMenu else null
    }

    private fun setupDoubleColumn(col: TableColumn<Working, Double?>, getter: (Working) -> Double?) {
        col.setCellValueFactory { SimpleObjectProperty(getter(it.value)) }
        col.setCellFactory {
            object : TableCell<Working, Double?>() {
                override fun updateItem(item: Double?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) "" else formatDouble(item)
                }
            }
        }
    }

    private fun createInteractiveCheckbox(col: TableColumn<Working, Boolean>, getter: (Working) -> Boolean, setter: (Working, Boolean) -> Unit, Colors: Boolean = false, checkType: Boolean = false) {
        col.setCellValueFactory { SimpleBooleanProperty(getter(it.value)) }
        col.setCellFactory {
            object : TableCell<Working, Boolean>() {
                val checkBox = CheckBox()

                init {
                    checkBox.setOnAction {
                        val working = tableView.items.getOrNull(index) ?: return@setOnAction
                        if (!checkBox.isDisabled) {
                            setter(working, checkBox.isSelected)
                            runOnFx {
                                try {
                                    api.updateWorking(authHeader(), working.id, working).await()
                                    updateStyle(checkBox.isSelected)
                                    tableView.refresh()
                                    recalculateSummaries()
                                } catch (e: Exception) {
                                    checkBox.isSelected = !checkBox.isSelected // Откат при ошибке
                                }
                            }
                        }
                    }
                    alignment = javafx.geometry.Pos.CENTER
                }

                override fun updateItem(item: Boolean?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                        style = ""
                    } else {
                        checkBox.isSelected = item
                        graphic = checkBox
                        updateStyle(item)
                        val working = tableView.items.getOrNull(index)
                        // Проверяем, нужно ли блокировать чекбокс для не-скважин или для зон только для чтения
                        if (checkType) {
                            val isWell = working?.workType?.name?.equals("скважина", ignoreCase = true) == true
                            checkBox.isDisable = !isWell || !canWriteArea(working?.area?.id)
                        } else {
                            checkBox.isDisable = !canWriteArea(working?.area?.id)
                        }
                    }
                }

                private fun updateStyle(isChecked: Boolean) {
                    style = if (tableView.items.getOrNull(index)?.workType?.name?.equals("скважина", ignoreCase = true) != true && checkType) {
                        "-fx-background-color: #e0e0e0;"
                    } else {
                        if (isChecked != Colors) "-fx-background-color: #a5d6a7;" else "-fx-background-color: #ef9a9a;"
                    }
                }
            }
        }
    }

    fun loadWorkings(onComplete: (() -> Unit)? = null) {
        runOnFx {
            try {
                val fetched = api.getWorkings(authHeader()).await().sortedBy { it.id }
                allWorkings.clear()
                allWorkings.addAll(fetched)
                
                applyFilter()

                val selectedArea = filterAreaCombo.value
                val areas = listOf("ВСЕ") + fetched.mapNotNull { it.area?.name }.distinct()
                filterAreaCombo.items = FXCollections.observableArrayList(areas)
                filterAreaCombo.value = selectedArea ?: "ВСЕ"
                filterAreaCombo.setOnAction { applyFilter() }

                if (!::tableFilter.isInitialized) {
                    rebuildTableFilter()
                }
                
                autoResizeColumns()
                updateAccessControls()

                // Вызываем колбэк после завершения всех обновлений
                onComplete?.invoke()
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить: ${e.message}")
            }
        }
    }

    private fun scheduleTableFilterRebuild() {
        if (tableFilterRebuildScheduled) return

        tableFilterRebuildScheduled = true
        Platform.runLater {
            tableFilterRebuildScheduled = false
            rebuildTableFilter()
        }
    }

    private fun rebuildTableFilter() {
        if (::tableFilter.isInitialized) {
            tableFilterItemsListener?.let { listener ->
                try {
                    tableFilter.filteredList.removeListener(listener)
                } catch (_: Exception) {
                }
            }
        }

        tableFilter = TableFilter.forTableView(workingsTable)
            .lazy(false)
            .apply()

        tableFilter.setSearchStrategy { input: String, target: String ->
            FilterParser.parse(input, target)
        }

        tableFilterItemsListener = ListChangeListener<Working> {
            recalculateSummaries()
        }
        tableFilter.filteredList.addListener(tableFilterItemsListener)

        recalculateSummaries()
    }

    private fun applyFilter() {

        val selected = filterAreaCombo.value

        val filtered =
            if (selected == null || selected == "ВСЕ") {
                allWorkings
            } else {
                allWorkings.filter {
                    it.area?.name == selected
                }
            }

        workingsList.setAll(filtered)
    }

    @FXML fun onAdd() {
        if (!canWriteAnyArea()) {
            showAlert("Нет прав", "У вас нет прав на создание выработок ни в одной из доступных зон")
            return
        }
        showWorkingForm(null)
    }

    @FXML fun onEdit() {
        val selected = workingsTable.selectionModel.selectedItem
        if (selected == null) return
        if (!canWriteArea(selected.area?.id)) {
            showAlert("Нет прав", "Для этой зоны доступны только права чтения")
            return
        }
        showWorkingForm(selected)
    }

    @FXML fun onDelete() {
        val selectedItems = workingsTable.selectionModel.selectedItems.toList()
        if (selectedItems.isNotEmpty()) {
            if (selectedItems.any { !canWriteArea(it.area?.id) }) {
                showAlert("Нет прав", "Выбранные выработки содержат зоны только для чтения")
                return
            }
            val alert = Alert(Alert.AlertType.CONFIRMATION, "Удалить ${selectedItems.size} записей?", ButtonType.YES, ButtonType.NO)
            if (alert.showAndWait().get() == ButtonType.YES) {
                val lastSelectedIndex = workingsTable.selectionModel.selectedIndex
                runOnFx {
                    try {
                        selectedItems.forEach { api.deleteWorking(authHeader(), it.id).awaitUnit() }
                        // Передаём колбэк для выделения строки после обновления таблицы
                        loadWorkings {
                            if (workingsList.isNotEmpty()) {
                                val newIdx = if (lastSelectedIndex >= workingsList.size) workingsList.size - 1 else lastSelectedIndex
                                workingsTable.selectionModel.select(newIdx)
                                workingsTable.requestFocus()
                            }
                        }
                    } catch (e: Exception) {
                        showAlert("Ошибка", e.message ?: "Ошибка удаления")
                    }
                }
            }
        }
    }

    private fun onAssignStructure() {
        val selectedItems = workingsTable.selectionModel.selectedItems
        if (selectedItems.isEmpty()) return
        if (selectedItems.any { !canWriteArea(it.area?.id) }) {
            showAlert("Нет прав", "Выбранные выработки содержат зоны только для чтения")
            return
        }

        val currentStructure = selectedItems.first().structure 
        val textInput = TextInputDialog(currentStructure)
        textInput.title = "Присвоить сооружение"
        textInput.headerText = "Введите название сооружения (оставьте пустым для удаления)"
        textInput.contentText = "Сооружение:"
        textInput.dialogPane.graphic = null
        textInput.showAndWait().ifPresent { structure ->
            runOnFx {
                try {
                    for (working in selectedItems) {
                        working.structure = if (structure.isBlank()) null else structure
                        api.updateWorking(authHeader(), working.id, working).await()
                    }
                    loadWorkings()
                } catch (e: Exception) {
                    showAlert("Ошибка", "Не удалось обновить сооружение: ${e.message}")
                }
            }
        }
    }

    private fun onAssignPlannedContractor() {
        val selectedItems = workingsTable.selectionModel.selectedItems
        if (selectedItems.isEmpty()) return
        if (selectedItems.any { !canWriteArea(it.area?.id) }) {
            showAlert("Нет прав", "Выбранные выработки содержат зоны только для чтения")
            return
        }

        val actualWorkings = selectedItems.filter { it.contractor != null }
    if (actualWorkings.isNotEmpty()) {
        val numbers = actualWorkings.joinToString(", ") { it.number }
        showAlert("Внимание!", "Скважины $numbers невозможно поставить в план (уже назначен фактический подрядчик)")
        return
    }

        runOnFx {
            try {
                val contractors = api.getContractors().await()
                // Создаём кастомный диалог
                val dialog = Dialog<RefContractor>()
                dialog.title = "Назначить подрядчика"
                dialog.headerText = "Выберите подрядчика для плановых работ"
                dialog.dialogPane.graphic = null

                val comboBox = ComboBox<RefContractor>()
                comboBox.items = FXCollections.observableArrayList(contractors)
                comboBox.converter = object : StringConverter<RefContractor>() {
                    override fun toString(obj: RefContractor?) = obj?.name ?: ""
                    override fun fromString(string: String?) = contractors.find { it.name == string }
                }
                comboBox.selectionModel.selectFirst()

                dialog.dialogPane.content = VBox(10.0, Label("Подрядчик:"), comboBox)
                dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

                dialog.resultConverter = javafx.util.Callback { buttonType ->
                    if (buttonType == ButtonType.OK) comboBox.value else null
                }

                dialog.showAndWait().ifPresent { contractor ->
                    val alreadyPlanned = selectedItems.filter { it.plannedContractor != null }
                    if (alreadyPlanned.isNotEmpty()) {
                        val confirm = Alert(Alert.AlertType.CONFIRMATION,
                            "Выбранные скважины уже имеют планового подрядчика. Заменить на ${contractor.name}?",
                            ButtonType.YES, ButtonType.NO)
                        confirm.dialogPane.graphic = null
                        if (confirm.showAndWait().get() != ButtonType.YES) return@ifPresent
                    }
                    runOnFx {
                        try {
                            for (working in selectedItems) {
                                working.plannedContractor = contractor
                                api.updateWorking(authHeader(), working.id, working).await()
                            }
                            loadWorkings()
                        } catch (e: Exception) {
                            showAlert("Ошибка", "Не удалось назначить подрядчика: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить список подрядчиков")
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
        Alert(Alert.AlertType.ERROR).apply { this.title = title; headerText = null; contentText = message; showAndWait() }
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
        loadWorkings() 
    }

    @FXML fun openProjectExcelImport() = openExcelImport(true)
    @FXML fun openActualExcelImport() = openExcelImport(false)

    private fun openExcelImport(isProject: Boolean) {
        val fileChooser = FileChooser().apply {
            title = "Выберите Excel файл"
            extensionFilters.add(FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx", "*.xlsm"))
        }
        val file = fileChooser.showOpenDialog(workingsTable.scene.window)
        if (file != null) {
            val loader = FXMLLoader(javaClass.getResource("/excelImport.fxml"))
            val root = loader.load<VBox>()
            // Передаем флаг isProject в контроллер импорта
            loader.getController<ExcelImportController>().initData(token, userRole, file, isProject) { loadWorkings() }
            Stage().apply {
                initModality(Modality.WINDOW_MODAL)
                initOwner(workingsTable.scene.window)
                scene = Scene(root)
                title = if (isProject) "Импорт ПРОЕКТНЫХ скважин" else "Импорт ФАКТИЧЕСКИХ скважин"
                showAndWait()
            }
        }
    }

    private fun autoResizeColumns() {
        fun resizeColumn(col: TableColumn<*, *>) {
            // Ширина заголовка + отступ
            var maxWidth = javafx.scene.text.Text(col.text).layoutBounds.width + 25.0
            // Проверяем данные в ячейках (до 50 строк для производительности)
            for (i in 0 until minOf(workingsTable.items.size, 50)) {
                val cellData = col.getCellData(i)?.toString() ?: ""
                val width = javafx.scene.text.Text(cellData).layoutBounds.width + 15.0
                if (width > maxWidth) maxWidth = width
            }
            col.prefWidth = maxWidth
            // Рекурсивно обрабатываем вложенные колонки
            col.columns.forEach { resizeColumn(it) }
        }
        workingsTable.columns.forEach { resizeColumn(it) }
    }

    private fun formatDouble(value: Double?, decimals: Int = 3): String {
        return if (value == null) "" else "%.${decimals}f".format(value).replace(",", ".")
    }

    private fun formatColumn(column: TableColumn<Working, Double?>, decimals: Int) {
        column.cellFactory = javafx.util.Callback {
            object : javafx.scene.control.TableCell<Working, Double?>() {
                override fun updateItem(item: Double?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) "" else String.format(java.util.Locale.US, "%.${decimals}f", item)
                }
            }
        }
    }

    fun setupEditableIntCol(column: TableColumn<Working, Int?>, setter: (Working, Int?) -> Unit) {
        column.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        column.setOnEditCommit { event ->
            val working = event.rowValue
            if (!canWriteArea(working.area?.id)) {
                showAlert("Нет прав", "Выбранная выработка находится в зоне только для чтения")
                return@setOnEditCommit
            }
            val newValue = event.newValue
            setter(working, newValue)
            recalculateSummaries()
              
            // Сразу сохраняем изменения на сервер
            runOnFx {
                try {
                    api.updateWorking(authHeader(), working.id, working).await()
                } catch (e: Exception) {
                    //showAlert("Ошибка", "Не удалось сохранить значение: ${e.message}")
                }
            }
        }
    }

    fun setupEditableDoubleColumn(column: TableColumn<Working, Double?>, setter: (Working, Double?) -> Unit) {
        column.cellFactory = TextFieldTableCell.forTableColumn(object : javafx.util.StringConverter<Double?>() {
            override fun toString(obj: Double?) = if (obj == null) "" else "%.1f".format(obj).replace(",", ".")
            override fun fromString(string: String) = string.replace(",", ".").toDoubleOrNull()
        })
        column.setOnEditCommit { event ->
            val working = event.rowValue
            if (!canWriteArea(working.area?.id)) {
                showAlert("Нет прав", "Выбранная выработка находится в зоне только для чтения")
                return@setOnEditCommit
            }
            val newValue = event.newValue
            setter(working, newValue)
            recalculateSummaries()
            runOnFx {
                try {
                    api.updateWorking(authHeader(), working.id, working).await()
                } catch (e: Exception) { /* show error */ }
            }
        }
    }

    @FXML
    fun openReportDialog() {
        val loader = FXMLLoader(javaClass.getResource("/report_dialog.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<ReportDialogController>()

        controller.initData(token, userRole, currentUser)

        val stage = Stage()
        stage.title = "Печать отчётов"
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.scene = Scene(root)
        stage.show()
    }

    @FXML fun openUsersEditor() {
        val loader = FXMLLoader(javaClass.getResource("/user_list.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<UserListController>()
        controller.initData(token)
        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Управление пользователями"
        stage.showAndWait()
    }
    
    private fun startAutoRefresh() {
        autoRefreshTimeline?.stop()

        autoRefreshTimeline = Timeline(
            KeyFrame(
                Duration.seconds(30.0),
                javafx.event.EventHandler {
                    loadWorkings()
                }
            )
        ).apply {
            cycleCount = Timeline.INDEFINITE
            play()
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshTimeline?.stop()
    }

    private fun setupSummaryRow() {
        val clipRect = Rectangle().apply {
            widthProperty().bind(summaryContainer.widthProperty())
            heightProperty().bind(summaryContainer.heightProperty())
        }
        summaryContainer.clip = clipRect

        workingsTable.visibleLeafColumns.addListener(ListChangeListener {
            rebuildSummaryUI()
            scheduleTableFilterRebuild()
            recalculateSummaries()
        })

        rebuildSummaryUI()
        rebuildTableFilter()

        workingsTable.items.addListener(ListChangeListener {
            recalculateSummaries()
        })
    }

    private fun bindSummaryScroll() {
        // Удаляем предыдущие слушатели, если они были (борьба с утечкой памяти)
        summaryScrollListener?.let { oldListener ->
            summaryScrollBar?.valueProperty()?.removeListener(oldListener)
        }
        summaryScrollBar = null
        summaryScrollListener = null

        // Функция для попытки поиска и привязки скроллбара
        fun tryFindAndBindScrollBar() {
            val scrollBars = workingsTable.lookupAll(".scroll-bar").filterIsInstance<ScrollBar>()
            val hBar = scrollBars.find { it.orientation == Orientation.HORIZONTAL }

            if (hBar != null) {
                summaryScrollBar = hBar
                // Создаем слушатель один раз
                val listener = ChangeListener<Number> { _, _, _ -> updateScroll(hBar) }
                summaryScrollListener = listener

                // Привязываем слушатели
                hBar.valueProperty().addListener(listener)
                hBar.maxProperty().addListener { _, _, _ -> updateScroll(hBar) }
                summaryHBox.layoutBoundsProperty().addListener { _, _, _ -> updateScroll(hBar) }
                summaryContainer.widthProperty().addListener { _, _, _ -> updateScroll(hBar) }

                // Инициализируем начальную позицию
                updateScroll(hBar)
            } else {
                // Скроллбар еще не найден, планируем повторную попытку
                Platform.runLater { tryFindAndBindScrollBar() }
            }
        }

        // Запускаем первоначальную попытку
        Platform.runLater { tryFindAndBindScrollBar() }
    }

    // Вынесенная логика обновления, принимающая конкретный ScrollBar
    private fun updateScroll(hBar: ScrollBar) {
        val max = hBar.max
        val viewportWidth = summaryContainer.width
        val contentWidth = summaryHBox.layoutBounds.width
        val scrollRange = (contentWidth - viewportWidth).coerceAtLeast(0.0)

        summaryHBox.translateX = if (max > 0.0 && scrollRange > 0.0) {
            -(hBar.value / max) * scrollRange
        } else {
            0.0
        }
    }

    private fun rebuildSummaryUI() {
        summaryHBox.children.clear()
        summaryLabels.clear()

        for (col in workingsTable.visibleLeafColumns) {
            val label = Label().apply {
                style = "-fx-font-weight: bold; -fx-text-fill: #333333; -fx-padding: 0 5 0 5; -fx-alignment: center;"
                prefWidthProperty().bind(col.widthProperty())
                minWidthProperty().bind(col.widthProperty())
                maxWidthProperty().bind(col.widthProperty())
            }
            summaryLabels[col] = label
            summaryHBox.children.add(label)
        }
    }

    private fun recalculateSummaries() {
        // Определяем источник данных: отфильтрованный список или базовый
        val items: List<Working> = if (::tableFilter.isInitialized) {
            tableFilter.filteredList.toList() // Используем отфильтрованный список
        } else {
            workingsTable.items // Fallback на базовый список
        }

        var sumFactH = 0.0
        var sumPlanH = 0.0
        var sumCat1_4 = 0.0
        var sumCat5_8 = 0.0
        var sumCat9_12 = 0.0
        var sumThawed = 0
        var sumFrozen = 0
        var sumRocky = 0
        var countThermal = 0

        // Пробегаем только по отфильтрованным (видимым) строкам
        for (w in items) {
            sumFactH += w.actualDepth ?: 0.0
            sumPlanH += w.plannedDepth ?: 0.0
            sumCat1_4 += w.cat1_4 ?: 0.0
            sumCat5_8 += w.cat5_8 ?: 0.0
            sumCat9_12 += w.cat9_12 ?: 0.0
            sumThawed += w.samplesThawed ?: 0
            sumFrozen += w.samplesFrozen ?: 0
            sumRocky += w.samplesRocky ?: 0
            if (w.thermalTube) countThermal++
        }

        // Очищаем текст во всех ячейках
        summaryLabels.values.forEach { it.text = "" }

        // Заполняем нужные ячейки, проверяя их наличие в visibleLeafColumns
        summaryLabels[colRowNumber]?.text = "${items.size}"
        summaryLabels[colActualDepth]?.text = formatDouble(sumFactH, 1)
        summaryLabels[colPlannedDepth]?.text = formatDouble(sumPlanH, 1)
        summaryLabels[colCat1_4]?.text = formatDouble(sumCat1_4, 1)
        summaryLabels[colCat5_8]?.text = formatDouble(sumCat5_8, 1)
        summaryLabels[colCat9_12]?.text = formatDouble(sumCat9_12, 1)
        
        summaryLabels[colSamplesThawed]?.text = sumThawed.toString()
        summaryLabels[colSamplesFrozen]?.text = sumFrozen.toString()
        summaryLabels[colSamplesRocky]?.text = sumRocky.toString()
        summaryLabels[colThermalTube]?.text = countThermal.toString()
    }

    @FXML fun openHistoryDialog() {
        val loader = FXMLLoader(javaClass.getResource("/history_dialog.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<HistoryController>()
        controller.initData(api, token)
        val stage = Stage()
        stage.title = "История изменений"
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root, 900.0, 500.0)
        stage.showAndWait()
    }

    @FXML fun openRecycleBinDialog() {
        val loader = FXMLLoader(javaClass.getResource("/recycle_bin_dialog.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<RecycleBinController>()
        controller.initData(api, token)
        val stage = Stage()
        stage.title = "Корзина удалённых"
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root, 600.0, 400.0)
        stage.showAndWait()
    }
}