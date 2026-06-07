package org.example.geoapp.controller

import org.example.geoapp.api.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx

enum class RefType(val title: String) {
    AREA("Участки"), WORK_TYPE("Типы выработок"), DRILLING_RIG("Буровые"),
    CONTRACTOR("Подрядчики"), GEOLOGIST("Геологи")
}

data class RefUiModel(val id: Long, val name: String, val alias: String?, val position: String?, val parent: RefContractor?, var comment: String?, val originalObj: Any)

class ReferenceEditorController {

    @FXML private lateinit var referenceTable: TableView<RefUiModel>
    @FXML private lateinit var colId: TableColumn<RefUiModel, String>
    @FXML private lateinit var colName: TableColumn<RefUiModel, String>
    @FXML private lateinit var colAlias: TableColumn<RefUiModel, String>
    @FXML private lateinit var colPosition: TableColumn<RefUiModel, String>
    @FXML private lateinit var colParent: TableColumn<RefUiModel, String>
    @FXML private lateinit var colComment: TableColumn<RefUiModel, String>

    @FXML private lateinit var nameField: TextField
    @FXML private lateinit var positionLabel: Label
    @FXML private lateinit var contractorLabel: Label
    @FXML private lateinit var aliasField: TextField
    @FXML private lateinit var positionField: TextField
    @FXML private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML private lateinit var errorLabel: Label
    @FXML private lateinit var deleteButton: Button
    @FXML private lateinit var saveButton: Button

    private lateinit var token: String
    private lateinit var currentType: RefType
    private val api = MainApp.api

    @FXML
    fun initialize() {
        referenceTable.isEditable = true
        colComment.cellFactory = TextFieldTableCell.forTableColumn()
        colComment.setOnEditCommit { event ->
            val uiModel = event.rowValue
            val newComment = event.newValue
            uiModel.comment = newComment
            runOnFx {
                try {
                    val tokenStr = "Bearer $token"
                    when (currentType) {
                        RefType.AREA -> api.updateArea(tokenStr, uiModel.id, (uiModel.originalObj as RefArea).copy(comment = newComment)).await()
                        RefType.WORK_TYPE -> api.updateWorkType(tokenStr, uiModel.id, (uiModel.originalObj as RefWorkType).copy(comment = newComment)).await()
                        RefType.DRILLING_RIG -> api.updateDrillingRig(tokenStr, uiModel.id, (uiModel.originalObj as RefDrillingRig).copy(comment = newComment)).await()
                        RefType.CONTRACTOR -> api.updateContractor(tokenStr, uiModel.id, (uiModel.originalObj as RefContractor).copy(comment = newComment)).await()
                        RefType.GEOLOGIST -> api.updateGeologist(tokenStr, uiModel.id, (uiModel.originalObj as RefGeologist).copy(comment = newComment)).await()
                    }
                } catch (e: Exception) {
                    errorLabel.text = "Ошибка при сохранении комментария: ${e.message}"
                }
            }
        }
    }

    fun initData(token: String, type: RefType) {
        this.token = token
        this.currentType = type

        colId.setCellValueFactory { SimpleStringProperty(it.value.id.toString()) }
        colName.setCellValueFactory { SimpleStringProperty(it.value.name) }
        colComment.setCellValueFactory { SimpleStringProperty(it.value.comment ?: "") }
        colAlias.setCellValueFactory { SimpleStringProperty(it.value.alias ?: "") }
        

        if (type == RefType.GEOLOGIST) {
            colParent.isVisible = true
            colParent.setCellValueFactory { SimpleStringProperty(it.value.parent?.name ?: "") }
            colPosition.isVisible = true
            colPosition.setCellValueFactory { SimpleStringProperty(it.value.position ?: "") }
            contractorLabel.isVisible = true
            contractorLabel.isManaged = true
            contractorCombo.isVisible = true
            contractorCombo.isManaged = true
            positionLabel.isVisible = true
            positionLabel.isManaged = true
            positionField.isVisible = true
            positionField.isManaged = true
            loadContractorsForCombo()
        }
        
        val showAlias = type == RefType.GEOLOGIST || type == RefType.DRILLING_RIG
        colAlias.isVisible = showAlias
        aliasField.isVisible = showAlias


        nameField.textProperty().addListener { _, _, _ -> updateButtons() }

        deleteButton.disableProperty().bind(referenceTable.selectionModel.selectedItemProperty().isNull)

        referenceTable.selectionModel.selectedItemProperty().addListener { _, _, selected ->
            if (selected != null) {
                // Режим редактирования
                nameField.text = selected.name
                if (type == RefType.GEOLOGIST) {
                    contractorCombo.value = selected.parent
                    positionField.text = selected.position ?: ""
                }
                aliasField.text = when (selected.originalObj) {
                    is RefGeologist -> selected.originalObj.alias ?: ""
                    is RefDrillingRig -> selected.originalObj.alias ?: ""
                    else -> ""
                }
                saveButton.text = "Сохранить изменения"
            } else {
                // Режим создания
                clearForm()
            }
            updateButtons()   // пересчитать доступность кнопки
        }

        loadData()
        clearForm()  // начальное состояние
    }

    private fun loadData() {
        runOnFx {
            try {
                val items = when (currentType) {
                    RefType.AREA -> api.getAreas().await().map { RefUiModel(it.id, it.name, null, null, null, it.comment, it) }
                    RefType.WORK_TYPE -> api.getWorkTypes().await().map { RefUiModel(it.id, it.name, null, null, null, it.comment, it) }
                    RefType.DRILLING_RIG -> api.getDrillingRigs().await().map { RefUiModel(it.id, it.name, it.alias, null, null, it.comment, it) }
                    RefType.CONTRACTOR -> api.getContractors().await().map { RefUiModel(it.id, it.name, null, null, null, it.comment, it) }
                    RefType.GEOLOGIST -> api.getGeologists().await().map { RefUiModel(it.id, it.name, it.alias, it.position, it.contractor, it.comment, it) }
                }.sortedBy { it.id }
                referenceTable.items = FXCollections.observableArrayList(items)
                autoSizeColumnsAndWindow()
            } catch (e: Exception) {
                errorLabel.text = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    private fun loadContractorsForCombo() {
        runOnFx {
            val contractors = api.getContractors().await()
            contractorCombo.items = FXCollections.observableArrayList(contractors)
            contractorCombo.converter = object : javafx.util.StringConverter<RefContractor>() {
                override fun toString(obj: RefContractor?) = obj?.name ?: ""
                override fun fromString(string: String) = contractorCombo.items.find { it.name == string }
            }
        }
    }

    private fun clearForm() {
        nameField.clear()
        aliasField.clear()
        contractorCombo.value = null
        positionField.text = ""
        errorLabel.text = ""
        saveButton.text = when (currentType) {
            RefType.AREA -> "Добавить участок"
            RefType.WORK_TYPE -> "Добавить тип"
            RefType.DRILLING_RIG -> "Добавить буровую"
            RefType.CONTRACTOR -> "Добавить подрядчика"
            RefType.GEOLOGIST -> "Добавить геолога"
        }
        updateButtons()
    }

    private fun updateButtons() {
        saveButton.isDisable = nameField.text.trim().isBlank()
    }

    @FXML fun onSave() {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            errorLabel.text = "Введите название!"
            return
        }

        val selected = referenceTable.selectionModel.selectedItem
        val id = selected?.id ?: 0L
        val tokenStr = "Bearer $token"

        runOnFx {
            try {
                val aliasStr = if (aliasField.isVisible) aliasField.text.trim().ifEmpty { null } else null
                when (currentType) {
                    RefType.AREA -> if (id == 0L) api.createArea(tokenStr, RefArea(name = name)).await() else api.updateArea(tokenStr, id, RefArea(name = name)).await()
                    RefType.WORK_TYPE -> if (id == 0L) api.createWorkType(tokenStr, RefWorkType(name = name)).await() else api.updateWorkType(tokenStr, id, RefWorkType(name = name)).await()
                    RefType.DRILLING_RIG -> {
                        if (id == 0L) api.createDrillingRig(tokenStr, RefDrillingRig(name = name, alias = aliasStr)).await()
                        else api.updateDrillingRig(tokenStr, id, RefDrillingRig(name = name, alias = aliasStr)).await()
                    }
                    RefType.CONTRACTOR -> if (id == 0L) api.createContractor(tokenStr, RefContractor(name = name)).await() else api.updateContractor(tokenStr, id, RefContractor(name = name)).await()
                    RefType.GEOLOGIST -> {
                        val c = contractorCombo.value
                        if (c == null) { errorLabel.text = "Выберите подрядчика!"; return@runOnFx }
                        if (id == 0L) api.createGeologist(tokenStr, RefGeologist(name = name, contractor = c, alias = aliasStr, position = positionField.text.trim())).await()
                        else api.updateGeologist(tokenStr, id, RefGeologist(name = name, contractor = c, alias = aliasStr, position = positionField.text.trim())).await()
                    }
                }
                onClearSelection()
                loadData()
            } catch (e: Exception) { errorLabel.text = "Ошибка сохранения: ${e.message}" }
        }
    }

    @FXML fun onDelete() {
        val selected = referenceTable.selectionModel.selectedItem ?: return
        runOnFx {
            try {
                val tokenStr = "Bearer $token"
                when (currentType) {
                    RefType.AREA -> api.deleteArea(tokenStr, selected.id).awaitUnit()
                    RefType.WORK_TYPE -> api.deleteWorkType(tokenStr, selected.id).awaitUnit()
                    RefType.DRILLING_RIG -> api.deleteDrillingRig(tokenStr, selected.id).awaitUnit()
                    RefType.CONTRACTOR -> api.deleteContractor(tokenStr, selected.id).awaitUnit()
                    RefType.GEOLOGIST -> api.deleteGeologist(tokenStr, selected.id).awaitUnit()
                }
                onClearSelection()
                loadData()
            } catch (e: Exception) { errorLabel.text = "Справочник используется. Нельзя удалить." }
        }
    }

    @FXML fun onClearSelection() {
        referenceTable.selectionModel.clearSelection()
        clearForm()
    }

    private fun autoSizeColumnsAndWindow() {
        referenceTable.columns.forEach { col ->
            var maxWidth = javafx.scene.text.Text(col.text).layoutBounds.width + 30.0
            for (i in 0 until minOf(referenceTable.items.size, 50)) {
                val cellData = col.getCellData(i)?.toString() ?: ""
                val width = javafx.scene.text.Text(cellData).layoutBounds.width + 20.0
                if (width > maxWidth) maxWidth = width
            }
            col.prefWidth = maxWidth
        }

        val totalWidth = referenceTable.columns.filter { it.isVisible }.sumOf { it.prefWidth } + 40.0
        val stage = referenceTable.scene?.window as? javafx.stage.Stage
        if (stage != null) {
            stage.minWidth = totalWidth
            if (stage.width < totalWidth) {
                stage.width = totalWidth
            }
        }
    }
}