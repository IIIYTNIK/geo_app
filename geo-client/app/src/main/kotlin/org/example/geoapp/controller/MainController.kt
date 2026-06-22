package org.example.geoapp.controller

import org.example.geoapp.api.UserDto
import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.Working
import org.example.geoapp.api.RefContractor
import org.example.geoapp.api.AccessLevel
import org.example.geoapp.api.UserAreaAccessDto
import javafx.beans.property.SimpleBooleanProperty
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
    @FXML private lateinit var colMedia: TableColumn<Working, String?>

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

    private var userAccessByAreaId: Map<Long, AccessLevel> = emptyMap()
    private val undoStack = ArrayDeque<UndoAction>(10)
    private val maxUndoSize = 10
    private var onSaveCallback: (Working) -> Unit = {}

    fun initData(token: String, role: String, user: UserDto) {
        this.token = token
        this.userRole = role
        this.currentUser = user
        adminMenu.isVisible = (role == "ROLE_ADMIN")
        loadWorkings {
            updateAccessControls()
            startAutoRefresh()
        }
    }

    private sealed class UndoAction {
        abstract suspend fun undo(api: GeoApi, token: String)
        data class Create(val working: Working) : UndoAction() {
            override suspend fun undo(api: GeoApi, token: String) {
                api.deleteWorking("Bearer $token", working.id).awaitUnit()
            }
        }
        data class Update(val oldWorking: Working, val newWorking: Working) : UndoAction() {
            override suspend fun undo(api: GeoApi, token: String) {
                api.updateWorking("Bearer $token", oldWorking.id, oldWorking).await()
            }
        }
        data class Delete(val working: Working) : UndoAction() {
            override suspend fun undo(api: GeoApi, token: String) {
                api.restoreWorking("Bearer $token", working.id).await()
            }
        }
        data class BatchUpdate(val updates: List<Pair<Working, Working>>) : UndoAction() {
        override suspend fun undo(api: GeoApi, token: String) {
            updates.forEach { (oldWorking, _) ->
                api.updateWorking("Bearer $token", oldWorking.id, oldWorking).await()
            }
        }
    }
    }

    
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
        
        colMedia.setCellValueFactory { SimpleStringProperty(it.value.mediaPath) }
        colMedia.setCellFactory {
            object : TableCell<Working, String?>() {
                val button = Button("🗀").apply {
                    style = "-fx-background-color: transparent; -fx-cursor: hand;"
                    setOnAction {
                        val path = item
                        if (!path.isNullOrBlank()) {
                            openFolderInExplorer(path)
                        }
                    }
                }
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item.isNullOrBlank()) {
                        graphic = null
                    } else {
                        graphic = button
                        alignment = javafx.geometry.Pos.CENTER
                        tooltip = Tooltip(item) // При наведении будет показывать полный путь
                    }
                }
            }
        }

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
        val contextMenu = ContextMenu()
        val assignStructureItem = MenuItem("Присвоить сооружение").apply { setOnAction { onAssignStructure() } }
        val assignPlannedContractorItem = MenuItem("Назначить подрядчика").apply { setOnAction { onAssignPlannedContractor() } }
        val assignMediaItem = MenuItem("Привязать медиафайлы").apply { setOnAction { openMediaWizard()}}
        val openMediaItem = MenuItem("Открыть медиафайлы").apply { 
            setOnAction {
                val selected = workingsTable.selectionModel.selectedItem
                if (selected?.mediaPath != null) {
                    openFolderInExplorer(selected.mediaPath!!)
                } else {
                    showAlert("Ошибка", "Для этой выработки не привязана папка.")
                }
            } 
        }
        // contextMenu.items.addAll()
        val editItem = MenuItem("Редактировать").apply { setOnAction { onEdit() } }
        val deleteItem = MenuItem("Удалить").apply { setOnAction { onDelete() } }
        contextMenu.items.addAll(assignStructureItem, assignPlannedContractorItem, SeparatorMenuItem(), assignMediaItem, openMediaItem, SeparatorMenuItem(), editItem, deleteItem)

        workingsTable.contextMenu = contextMenu

        // Слушатель на выделение и обновление состояния кнопок
        workingsTable.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateAccessControls()
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
            newScene?.accelerators?.put(KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN)) {undoLastAction()}
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
                        !canWriteArea(item.area?.id) -> styleClass.add("read-only-row")
                        item.isProject != true -> styleClass.add("project-filled")
                        item.isProject -> styleClass.add("project-empty")
                        else -> styleClass.add("actual")
                    }
                }
            }
        }

        disableColumnReordering(workingsTable.columns)
        
        setupSummaryRow()
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
                        val working = tableView.items[index]

                        if (!canWriteArea(working.area?.id)) {
                            checkBox.isSelected = !checkBox.isSelected // Визуально возвращаем галочку на место
                            return@setOnAction
                        }

                        if (!checkBox.isDisabled) {
                            val oldCopy = working.copy()
                            setter(working, checkBox.isSelected)
                            val newCopy = working.copy()
                            runOnFx {
                                try {
                                    api.updateWorking("Bearer $token", working.id, working).await()
                                    undoStack.addLast(UndoAction.Update(oldWorking = oldCopy, newWorking = newCopy))
                                    if (undoStack.size > maxUndoSize) {
                                        undoStack.removeFirst()
                                    }

                                    updateStyle(checkBox.isSelected)
                                    tableView.refresh()
                                    recalculateSummaries()
                                } catch (e: Exception) {
                                    checkBox.isSelected = !checkBox.isSelected
                                    setter(working, checkBox.isSelected)
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
                        // Проверяем, нужно ли блокировать чекбокс для не-скважин
                        if (checkType) {
                            val working = tableView.items[index]
                            val isWell = working.workType?.name?.equals("скважина", ignoreCase = true) == true
                            checkBox.isDisable = !isWell
                            
                        } else {
                            checkBox.isDisable = false
                        }
                    }
                }

                private fun updateStyle(isChecked: Boolean) {//Вначале проверка на не выделение для шурфа и расчистки
                    style = if (tableView.items[index].workType?.name?.equals("скважина", ignoreCase = true) != true && checkType){"-fx-background-color: #e0e0e0;"}
                    else{ //После проверка на значение чекбокса и выделение
                        if (isChecked != Colors) "-fx-background-color: #a5d6a7;" else "-fx-background-color: #ef9a9a;"
                    }
                }
            }
        }
    }

    fun loadWorkings(onComplete: (() -> Unit)? = null) {
        val selectedIds = workingsTable.selectionModel.selectedItems.mapNotNull { it?.id }.toSet() // Запоминаем ID выделенных элементов до того, как таблица обновится
        runOnFx {
            try {

                try {
                    val accessList = api.getCurrentUserAccess("Bearer $token").await()
                    userAccessByAreaId = accessList.associate { it.areaId to it.accessLevel }
                } catch (e: Exception) {
                    e.printStackTrace() // Если сервер моргнул, оставляем права какие были
                }

                val fetched = api.getWorkings("Bearer $token").await().sortedBy { it.id }
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
                
                restoreSelection(selectedIds) // Восстанавливаем выделение после того, как новые данные отрисовались
                autoResizeColumns()
                updateAccessControls() // Обновляем доступность кнопок по новым правам
                workingsTable.refresh() // Перерисовываем серые/зеленые строки

                onComplete?.invoke() // Вызываем колбэк после завершения всех обновлений
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить: ${e.message}")
            }
        }
    }

    private fun restoreSelection(savedIds: Set<Long>) {
        if (savedIds.isEmpty()) return

        val indicesToSelect = workingsTable.items.mapIndexedNotNull { index, working ->  // Находим индексы новых строк, чьи ID совпадают с сохраненными
                if (savedIds.contains(working.id)) index else null 
            }

        if (indicesToSelect.isNotEmpty()) {
            workingsTable.selectionModel.clearSelection() // На всякий случай очищаем текущее состояние
            
            val firstIndex = indicesToSelect[0]
            val restIndices = indicesToSelect.drop(1).toIntArray()
            
            workingsTable.selectionModel.selectIndices(firstIndex, *restIndices)
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

        tableFilter = TableFilter.forTableView(workingsTable).lazy(false).apply()
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

    // Функция блокировки перетаскивания колонок
    private fun disableColumnReordering(columns: List<TableColumn<Working, *>>) {
        for (col in columns) {
            col.isReorderable = false // Запрещаем перетаскивание
            if (col.columns.isNotEmpty()) {
                // Рекурсивно отключаем для вложенных колонок (групп)
                disableColumnReordering(col.columns)
            }
        }
    }

    @FXML fun onAdd() {
        if (!canWriteAnyArea()) return
        showWorkingForm(null) { created ->
            undoStack.addLast(UndoAction.Create(created))
            if (undoStack.size > maxUndoSize) undoStack.removeFirst()
        }
    }

    @FXML fun onEdit() {
        val selected = workingsTable.selectionModel.selectedItem ?: return
        if (!canWriteArea(selected.area?.id)) return
        val oldCopy = selected.copy()   // сохраняем состояние до редактирования
        showWorkingForm(selected) { updated ->
            undoStack.addLast(UndoAction.Update(oldCopy, updated))
            if (undoStack.size > maxUndoSize) undoStack.removeFirst()
        }
    }

    @FXML fun onDelete() {
        val selectedItems = workingsTable.selectionModel.selectedItems.toList()
        if (selectedItems.isNotEmpty()) {
            // Проверяем, есть ли среди выбранных хотя бы одна без прав WRITE
            val forbidden = selectedItems.any { !canWriteArea(it.area?.id) }
            if (forbidden) {
                //showAlert("Нет прав", "Выбранные выработки содержат зоны только для чтения")
                return
            }
            val alert = Alert(Alert.AlertType.CONFIRMATION, "Удалить ${selectedItems.size} записей?", ButtonType.YES, ButtonType.NO)
            if (alert.showAndWait().get() == ButtonType.YES) {
                val lastSelectedIndex = workingsTable.selectionModel.selectedIndex
                runOnFx {
                    try {
                        selectedItems.forEach { 
                            undoStack.addLast(UndoAction.Delete(it.copy()))
                            api.deleteWorking("Bearer $token", it.id).awaitUnit() 
                        }
                        if (undoStack.size > maxUndoSize) undoStack.removeFirst()
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
        val selectedItems = workingsTable.selectionModel.selectedItems.toList()
        if (selectedItems.isEmpty()) return

        val currentStructure = selectedItems.first().structure ?: ""

        val textInput = TextInputDialog(currentStructure)
        textInput.title = "Присвоить сооружение"
        textInput.headerText = "Введите название сооружения (оставьте пустым для удаления)"
        textInput.contentText = "Сооружение:"
        textInput.dialogPane.graphic = null

        val result = textInput.showAndWait()
        if (result.isEmpty) return

        val newStructure = result.get().trim().ifBlank { null } // Пустая строка превращается в null

        runOnFx {
            try {
                val changes = mutableListOf<Pair<Working, Working>>()

                for (working in selectedItems) {
                    val oldCopy = working.copy()
                    working.structure = newStructure
                    api.updateWorking("Bearer $token", working.id, working).await()
                    changes += oldCopy to working.copy()
                }

                undoStack.addLast( UndoAction.BatchUpdate(changes))
                loadWorkings()
            } catch (e: Exception) {
                showAlert( "Ошибка", "Не удалось обновить сооружение: ${e.message}"
                )
            }
        }
    }

    private fun onAssignPlannedContractor() {
        val selectedItems = workingsTable.selectionModel.selectedItems.toList()
        if (selectedItems.isEmpty()) return

        val actualWorkings = selectedItems.filter { it.contractor != null }
        if (actualWorkings.isNotEmpty()) {
            val numbers = actualWorkings.joinToString(", ") { it.number }
            showAlert("Внимание!", "Скважины $numbers невозможно поставить в план (уже назначен фактический подрядчик)")
            return
        }

        runOnFx {
            try {
                val contractors = api.getContractors().await()

                val dialog = Dialog<ButtonType>() 
                dialog.title = "Назначить подрядчика"
                dialog.headerText = "Выберите планового подрядчика"
                dialog.dialogPane.graphic = null

                val comboBox = ComboBox<RefContractor?>()
                val values = mutableListOf<RefContractor?>()
                values.add(null) // пункт очистки
                values.addAll(contractors)

                comboBox.items = FXCollections.observableArrayList(values)
                comboBox.converter = object : StringConverter<RefContractor?>() {
                    override fun toString(obj: RefContractor?): String {
                        return obj?.name ?: "<Не указано>"
                    }
                    override fun fromString(string: String?): RefContractor? {
                        return contractors.find { it.name == string }
                    }
                }
                comboBox.selectionModel.selectFirst()
                dialog.dialogPane.content = VBox(10.0, Label("Подрядчик:"), comboBox)
                dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

                val result = dialog.showAndWait()
                if (result.isEmpty || result.get() != ButtonType.OK) return@runOnFx
                val selectedContractor = comboBox.value

                val alreadyPlanned = selectedItems.any { it.plannedContractor != null }

                if (alreadyPlanned) {
                    val message =
                        if (selectedContractor == null)
                            "У выбранных скважин уже есть плановый подрядчик. Очистить его?"
                        else
                            "У выбранных скважин уже есть плановый подрядчик. Заменить на ${selectedContractor.name}?"

                    val confirm = Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO)
                    confirm.dialogPane.graphic = null
                    if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                        return@runOnFx
                    }
                }

                try {
                    val changes = mutableListOf<Pair<Working, Working>>()
                    for (working in selectedItems) {
                        val oldCopy = working.copy()
                        working.plannedContractor = selectedContractor
                        api.updateWorking("Bearer $token", working.id, working).await()
                        changes += oldCopy to working.copy()
                    }
                    undoStack.addLast(UndoAction.BatchUpdate(changes))
                    loadWorkings()
                } catch (e: Exception) {
                    showAlert("Ошибка", "Не удалось назначить подрядчика: ${e.message}")
                }

            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить список подрядчиков")
            }
        }
    }

    private fun showWorkingForm(working: Working?, onSuccess: ((Working) -> Unit)? = null) {
        val loader = FXMLLoader(javaClass.getResource("/workingForm.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<WorkingFormController>()
        controller.initData(token, working) { savedWorking ->
            loadWorkings()
            onSuccess?.invoke(savedWorking)
        }

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = if (working == null) "Новая выработка" else "Редактирование"
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
    }

    private fun showAlert(title: String, message: String) {
        Alert(Alert.AlertType.ERROR).apply { this.title = title; headerText = null; contentText = message; showAndWait() }
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
            val newValue = event.newValue

            if (!canWriteArea(working.area?.id)) {
                //showAlert("Нет прав", "Выбранная выработка находится в зоне только для чтения")
                return@setOnEditCommit
            }

            setter(working, newValue)
            recalculateSummaries()
              
            // Сразу сохраняем изменения на сервер
            runOnFx {
                try {
                    api.updateWorking("Bearer $token", working.id, working).await()
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
            val newValue = event.newValue

            if (!canWriteArea(working.area?.id)) {
                //showAlert("Нет прав", "Выбранная выработка находится в зоне только для чтения")
                return@setOnEditCommit
            }

            setter(working, newValue)
            recalculateSummaries()
            runOnFx {
                try {
                    api.updateWorking("Bearer $token", working.id, working).await()
                } catch (e: Exception) { /* show error */ }
            }
        }
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

    private fun pauseAutoRefresh() {
        autoRefreshTimeline?.pause()
    }

    private fun resumeAutoRefresh() {
        autoRefreshTimeline?.play()
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

        bindSummaryScroll()
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
        summaryLabels[colRowNumber]?.text = "Σ = ${items.size}"
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


    private fun loadUserAccess(onComplete: () -> Unit) {
        runOnFx {
            try {
                val accessList = api.getCurrentUserAccess("Bearer $token").await()
                userAccessByAreaId = accessList.associate { it.areaId to it.accessLevel }
                onComplete()
            } catch (e: Exception) {
                // Если не удалось загрузить права (например, старый токен), всё равно продолжаем
                // но кнопки будут заблокированы
                e.printStackTrace()
                userAccessByAreaId = emptyMap()
                onComplete()
            }
        }
    }

    private fun canWriteArea(areaId: Long?): Boolean {
        if (areaId == null) return true // Если нет привязки к зоне, считаем, что это общая зона и права на неё есть
        return userAccessByAreaId[areaId] == AccessLevel.WRITE
    }

    private fun canWriteSelectedRow(): Boolean {
        val selected = workingsTable.selectionModel.selectedItem
        return canWriteArea(selected?.area?.id)
    }

    private fun canWriteAnyArea(): Boolean {
        return userAccessByAreaId.values.any { it == AccessLevel.WRITE }
    }

    private fun updateAccessControls() {
        val canWrite = canWriteSelectedRow()
        editButton.isDisable = !canWrite
        deleteButton.isDisable = !canWrite
        addButton.isDisable = !canWriteAnyArea()
        // Контекстное меню показываем только если есть права на запись выбранной строки
        //workingsTable.contextMenu?.setDisable(!canWrite)//TODO: ДОДЕЛАТЬ
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
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
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
            
            pauseAutoRefresh()
            try {
                Stage().apply {
                    initModality(Modality.WINDOW_MODAL)
                    initOwner(workingsTable.scene.window)
                    scene = Scene(root)
                    title = if (isProject) "Импорт ПРОЕКТНЫХ скважин" else "Импорт ФАКТИЧЕСКИХ скважин"
                    showAndWait()
                }
            } finally {
                resumeAutoRefresh()
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
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
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
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
        loadWorkings() // на случай, если были изменены права текущего пользователя
    }

    @FXML fun openRecycleBinDialog() {
        val loader = FXMLLoader(javaClass.getResource("/recycle_bin.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<RecycleBinController>()
        controller.initData(api, token)
        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Корзина"
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
        loadWorkings() // обновить таблицу после возможного восстановления
    }

    @FXML fun openHistoryDialog() {
        val loader = FXMLLoader(javaClass.getResource("/history_dialog.fxml"))
        val root = loader.load<VBox>()
        val controller = loader.getController<HistoryController>()
        controller.initData(api, token)
        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "История изменений"
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
        loadWorkings()
    }

    private fun undoLastAction() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeLast()
        runOnFx {
            try {
                action.undo(api, token)
                loadWorkings()
                //showAlert("Отмена", "Действие отменено")
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось отменить действие: ${e.message}")
            }
        }
    }

    // Открытие папки средствами Windows
    @FXML private fun openFolderInExplorer(path: String) {
        val file = java.io.File(path)
        if (file.exists() && file.isDirectory) {
            try {
                java.awt.Desktop.getDesktop().open(file)
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось открыть папку: ${e.message}")
            }
        } else {
            showAlert("Ошибка", "Путь не найден или недоступен:\n$path")
        }
    }

    // Вызов окна Мастера привязки медиафайлов для выбранных выработок
    @FXML private fun openMediaWizard() {
        val selectedItems = workingsTable.selectionModel.selectedItems.toList()
        if (selectedItems.isEmpty()) return

        val loader = FXMLLoader(javaClass.getResource("/media_wizard.fxml"))
        val root = loader.load<VBox>()
        
        // Пока мы передаем пустую логику, так как MediaWizardController создадим на следующем этапе
        val controller = loader.getController<MediaWizardController>()
        controller.initData(token, selectedItems) { loadWorkings() }

        val stage = Stage()
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(workingsTable.scene.window)
        stage.scene = Scene(root)
        stage.title = "Привязка медиафайлов (${selectedItems.size} шт.)"
        
        pauseAutoRefresh()
        try {
            stage.showAndWait()
        } finally {
            resumeAutoRefresh()
        }
    }

}