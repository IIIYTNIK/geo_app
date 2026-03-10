package org.example.geoapp.controller

import com.example.geoapp.api.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx

enum class RefType(val title: String) {
    AREA("Участки"), WORK_TYPE("Типы выработок"), DRILLING_RIG("Буровые"),
    CONTRACTOR("Подрядчики"), GEOLOGIST("Геологи")
}

data class RefUiModel(val id: Long, val name: String, val parent: RefContractor?, val originalObj: Any)

class ReferenceEditorController {

    @FXML private lateinit var referenceTable: TableView<RefUiModel>
    @FXML private lateinit var colId: TableColumn<RefUiModel, String>
    @FXML private lateinit var colName: TableColumn<RefUiModel, String>
    @FXML private lateinit var colParent: TableColumn<RefUiModel, String>

    @FXML private lateinit var nameField: TextField
    @FXML private lateinit var contractorLabel: Label
    @FXML private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML private lateinit var errorLabel: Label
    @FXML private lateinit var deleteButton: Button
    @FXML private lateinit var saveButton: Button

    private lateinit var token: String
    private lateinit var currentType: RefType
    private val api = MainApp.api

    fun initData(token: String, type: RefType) {
        this.token = token
        this.currentType = type

        colId.setCellValueFactory { SimpleStringProperty(it.value.id.toString()) }
        colName.setCellValueFactory { SimpleStringProperty(it.value.name) }
        
        if (type == RefType.GEOLOGIST) {
            colParent.isVisible = true
            colParent.setCellValueFactory { SimpleStringProperty(it.value.parent?.name ?: "") }
            contractorLabel.isVisible = true
            contractorLabel.isManaged = true
            contractorCombo.isVisible = true
            contractorCombo.isManaged = true
            loadContractorsForCombo()
        }

        // Слушатель выделения строки
        referenceTable.selectionModel.selectedItemProperty().addListener { _, _, selected ->
            if (selected != null) {
                // Если выбрали строку - переходим в режим РЕДАКТИРОВАНИЯ
                nameField.text = selected.name
                if (type == RefType.GEOLOGIST) contractorCombo.value = selected.parent
                deleteButton.isDisable = false
                saveButton.text = "Сохранить изменения" 
            } else {
                // Если ничего не выбрано - переходим в режим СОЗДАНИЯ
                saveButton.text = "Добавить новую запись"
                deleteButton.isDisable = true
            }
        }

        loadData()
    }

    private fun loadData() {
        runOnFx {
            try {
                val items = mutableListOf<RefUiModel>()
                when (currentType) {
                    RefType.AREA -> api.getAreas().await().forEach { items.add(RefUiModel(it.id, it.name, null, it)) }
                    RefType.WORK_TYPE -> api.getWorkTypes().await().forEach { items.add(RefUiModel(it.id, it.name, null, it)) }
                    RefType.DRILLING_RIG -> api.getDrillingRigs().await().forEach { items.add(RefUiModel(it.id, it.name, null, it)) }
                    RefType.CONTRACTOR -> api.getContractors().await().forEach { items.add(RefUiModel(it.id, it.name, null, it)) }
                    RefType.GEOLOGIST -> api.getGeologists().await().forEach { items.add(RefUiModel(it.id, it.name, it.contractor, it)) }
                }
                referenceTable.items = FXCollections.observableArrayList(items)
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
                when (currentType) {
                    RefType.AREA -> if (id == 0L) api.createArea(tokenStr, RefArea(name = name)).await() else api.updateArea(tokenStr, id, RefArea(name = name)).await()
                    RefType.WORK_TYPE -> if (id == 0L) api.createWorkType(tokenStr, RefWorkType(name = name)).await() else api.updateWorkType(tokenStr, id, RefWorkType(name = name)).await()
                    RefType.DRILLING_RIG -> if (id == 0L) api.createDrillingRig(tokenStr, RefDrillingRig(name = name)).await() else api.updateDrillingRig(tokenStr, id, RefDrillingRig(name = name)).await()
                    RefType.CONTRACTOR -> if (id == 0L) api.createContractor(tokenStr, RefContractor(name = name)).await() else api.updateContractor(tokenStr, id, RefContractor(name = name)).await()
                    RefType.GEOLOGIST -> {
                        val c = contractorCombo.value
                        if (c == null) { errorLabel.text = "Выберите подрядчика!"; return@runOnFx }
                        if (id == 0L) api.createGeologist(tokenStr, RefGeologist(name = name, contractor = c)).await() 
                        else api.updateGeologist(tokenStr, id, RefGeologist(name = name, contractor = c)).await()
                    }
                }
                onClearSelection() // Сбрасываем форму после успешного сохранения
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
        nameField.clear()
        contractorCombo.value = null
        errorLabel.text = ""
        // Кнопки изменятся автоматически благодаря слушателю selectedItemProperty
    }
}