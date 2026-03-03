package org.example.geoapp.controller

import com.example.geoapp.api.*
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.await
import java.time.LocalDate

class WorkingFormController {

    @FXML
    private lateinit var areaCombo: ComboBox<RefArea>
    @FXML
    private lateinit var workTypeCombo: ComboBox<RefWorkType>
    @FXML
    private lateinit var geologistCombo: ComboBox<RefGeologist>
    @FXML
    private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML
    private lateinit var drillingRigCombo: ComboBox<RefDrillingRig>
    @FXML
    private lateinit var numberField: TextField
    @FXML
    private lateinit var plannedXField: TextField
    @FXML
    private lateinit var plannedYField: TextField
    @FXML
    private lateinit var plannedZField: TextField
    @FXML
    private lateinit var actualXField: TextField
    @FXML
    private lateinit var actualYField: TextField
    @FXML
    private lateinit var actualZField: TextField
    @FXML
    private lateinit var depthField: TextField
    @FXML
    private lateinit var startDatePicker: DatePicker
    @FXML
    private lateinit var endDatePicker: DatePicker
    @FXML
    private lateinit var additionalInfoArea: TextArea
    @FXML
    private lateinit var saveButton: Button
    @FXML
    private lateinit var cancelButton: Button
    @FXML
    private lateinit var errorLabel: Label

    private lateinit var token: String
    private var working: Working? = null
    private var onSaveCallback: () -> Unit = {}

    private val api = MainApp.api

    fun initData(token: String, working: Working?, onSave: () -> Unit) {
        this.token = token
        this.working = working
        this.onSaveCallback = onSave
        loadReferences()
        if (working != null) {
            fillFields()
        }
    }

    private fun loadReferences() {
        runOnFx {
            try {
                val areas = api.getAreas().await()
                val workTypes = api.getWorkTypes().await()
                val geologists = api.getGeologists().await()
                val contractors = api.getContractors().await()
                val drillingRigs = api.getDrillingRigs().await()

                areaCombo.items = FXCollections.observableArrayList(areas)
                areaCombo.setupNameConverter { it.name }

                workTypeCombo.items = FXCollections.observableArrayList(workTypes)
                workTypeCombo.setupNameConverter { it.name }

                geologistCombo.items = FXCollections.observableArrayList(geologists)
                geologistCombo.setupNameConverter { it.name }

                contractorCombo.items = FXCollections.observableArrayList(contractors)
                contractorCombo.setupNameConverter { it.name }

                drillingRigCombo.items = FXCollections.observableArrayList(drillingRigs)
                drillingRigCombo.setupNameConverter { it.name }

                // Установка выбранных значений, если редактируем
                if (working != null) {
                    areaCombo.selectionModel.select(working?.area)
                    workTypeCombo.selectionModel.select(working?.workType)
                    geologistCombo.selectionModel.select(working?.geologist)
                    contractorCombo.selectionModel.select(working?.contractor)
                    drillingRigCombo.selectionModel.select(working?.drillingRig)
                }

            } catch (e: Exception) {
                errorLabel.text = "Ошибка загрузки справочников: ${e.message}"
            }
        }
    }

    private fun fillFields() {
        numberField.setText(working?.number)
        plannedXField.setText(working?.plannedX?.toString())
        plannedYField.setText(working?.plannedY?.toString())
        plannedZField.setText(working?.plannedZ?.toString())
        actualXField.setText(working?.actualX?.toString())
        actualYField.setText(working?.actualY?.toString())
        actualZField.setText(working?.actualZ?.toString())
        depthField.setText(working?.depth?.toString())
        startDatePicker.value = working?.startDate?.let { LocalDate.parse(it) }
        endDatePicker.value = working?.endDate?.let { LocalDate.parse(it) }
        additionalInfoArea.setText(working?.additionalInfo)
    }

    @FXML
    fun onSave() {
        if (numberField.text.isBlank()) {
            errorLabel.text = "Номер обязателен"
            return
        }

        val newWorking = Working(
            id = working?.id ?: 0,
            area = areaCombo.selectionModel.selectedItem,
            workType = workTypeCombo.selectionModel.selectedItem,
            number = numberField.text.trim(),
            plannedX = plannedXField.text.toDoubleOrNull(),
            plannedY = plannedYField.text.toDoubleOrNull(),
            plannedZ = plannedZField.text.toDoubleOrNull(),
            actualX = actualXField.text.toDoubleOrNull(),
            actualY = actualYField.text.toDoubleOrNull(),
            actualZ = actualZField.text.toDoubleOrNull(),
            depth = depthField.text.toDoubleOrNull(),
            startDate = startDatePicker.value?.toString(),
            endDate = endDatePicker.value?.toString(),
            geologist = geologistCombo.selectionModel.selectedItem,
            contractor = contractorCombo.selectionModel.selectedItem,
            drillingRig = drillingRigCombo.selectionModel.selectedItem,
            additionalInfo = additionalInfoArea.text.ifBlank { null }
        )

        saveButton.isDisable = true
        errorLabel.text = ""

        runOnFx {
            try {
                if (working == null) {
                    api.createWorking("Bearer $token", newWorking).await()
                } else {
                    api.updateWorking("Bearer $token", working!!.id, newWorking).await()
                }
                onSaveCallback()
                close()
            } catch (e: Exception) {
                errorLabel.text = if (e.message?.contains("HTTP 409") == true) {
                    "Номер уже существует"
                } else {
                    "Ошибка сохранения: ${e.message}"
                }
                saveButton.isDisable = false
            }
        }
    }

    @FXML
    fun onCancel() {
        close()
    }

    private fun close() {
        (areaCombo.scene.window as Stage).close()
    }

    // Вспомогательная функция для настройки отображения ComboBox
    private fun <T> ComboBox<T>.setupNameConverter(extractor: (T) -> String) {
        converter = object : javafx.util.StringConverter<T>() {
            override fun toString(obj: T?): String = obj?.let(extractor) ?: ""
            override fun fromString(string: String): T? {
                return items.find { extractor(it) == string }
            }
        }
    }
}

