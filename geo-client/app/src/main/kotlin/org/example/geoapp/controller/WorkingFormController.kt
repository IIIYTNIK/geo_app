package org.example.geoapp.controller

import com.example.geoapp.api.*
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import java.time.LocalDate
import java.lang.reflect.Method

class WorkingFormController {

    @FXML private lateinit var areaCombo: ComboBox<RefArea>
    @FXML private lateinit var workTypeCombo: ComboBox<RefWorkType>
    @FXML private lateinit var geologistCombo: ComboBox<RefGeologist>
    @FXML private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML private lateinit var drillingRigCombo: ComboBox<RefDrillingRig>
    @FXML private lateinit var numberField: TextField
    @FXML private lateinit var plannedXField: TextField
    @FXML private lateinit var plannedYField: TextField
    @FXML private lateinit var plannedZField: TextField
    @FXML private lateinit var actualXField: TextField
    @FXML private lateinit var actualYField: TextField
    @FXML private lateinit var actualZField: TextField
    @FXML private lateinit var depthField: TextField
    @FXML private lateinit var casingField: TextField
    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker
    @FXML private lateinit var additionalInfoArea: TextArea

    @FXML private lateinit var mmg1TopField: TextField
    @FXML private lateinit var mmg1BottomField: TextField
    @FXML private lateinit var mmg2TopField: TextField
    @FXML private lateinit var mmg2BottomField: TextField
    @FXML private lateinit var gwAppearLogField: TextField
    @FXML private lateinit var gwStableLogField: TextField
    @FXML private lateinit var gwStableAbsField: TextField
    @FXML private lateinit var gwStableRelField: TextField
    @FXML private lateinit var gwStableAbsFinalField: TextField

    @FXML private lateinit var coreRecoveryField: TextField
    @FXML private lateinit var contractorExtraIndexField: TextField
    @FXML private lateinit var actField: TextField
    @FXML private lateinit var actNumberField: TextField
    @FXML private lateinit var thermalTubeField: TextField

    @FXML private lateinit var saveButton: Button
    @FXML private lateinit var cancelButton: Button
    @FXML private lateinit var errorLabel: Label

    private lateinit var token: String
    private var working: Working? = null
    private var onSaveCallback: () -> Unit = {}

    private val api = MainApp.api

    fun initData(token: String, working: Working?, onSave: () -> Unit) {
        this.token = token
        this.working = working
        this.onSaveCallback = onSave
        loadReferences()
        setupContractorAutoFill()
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

            } catch (e: Exception) {
                errorLabel.text = "Ошибка загрузки справочников: ${e.message}"
            }
        }
    }

    private fun fillFields() {
        // Устранение NPE при редактировании пустых полей через elvis operator ?: ""
        numberField.text = working?.number ?: ""

        plannedXField.text = working?.plannedX?.toString() ?: ""
        plannedYField.text = working?.plannedY?.toString() ?: ""
        plannedZField.text = working?.plannedZ?.toString() ?: ""

        actualXField.text = working?.actualX?.toString() ?: ""
        actualYField.text = working?.actualY?.toString() ?: ""
        actualZField.text = working?.actualZ?.toString() ?: ""

        depthField.text = working?.depth?.toString() ?: ""
        casingField.text = working?.casing ?: ""

        startDatePicker.value = working?.startDate?.let { LocalDate.parse(it) }
        endDatePicker.value = working?.endDate?.let { LocalDate.parse(it) }

        additionalInfoArea.text = working?.additionalInfo ?: ""

        mmg1TopField.text = working?.mmg1Top?.toString() ?: ""
        mmg1BottomField.text = working?.mmg1Bottom?.toString() ?: ""
        mmg2TopField.text = working?.mmg2Top?.toString() ?: ""
        mmg2BottomField.text = working?.mmg2Bottom?.toString() ?: ""

        gwAppearLogField.text = working?.gwAppearLog?.toString() ?: ""
        gwStableLogField.text = working?.gwStableLog?.toString() ?: ""
        gwStableAbsField.text = working?.gwStableAbs?.toString() ?: ""
        gwStableRelField.text = working?.gwStableRel?.toString() ?: ""
        gwStableAbsFinalField.text = working?.gwStableAbsFinal?.toString() ?: ""

        coreRecoveryField.text = working?.coreRecovery?.toString() ?: ""

        contractorExtraIndexField.text = working?.contractorExtraIndex ?: ""
        contractorExtraIndexField.isEditable = false

        actField.text = working?.act ?: ""
        actNumberField.text = working?.actNumber ?: ""
        thermalTubeField.text = working?.thermalTube ?: ""

        areaCombo.selectionModel.select(working?.area)
        workTypeCombo.selectionModel.select(working?.workType)
        geologistCombo.selectionModel.select(working?.geologist)
        contractorCombo.selectionModel.select(working?.contractor)
        drillingRigCombo.selectionModel.select(working?.drillingRig)
    }
    
    private fun setupContractorAutoFill() {
        contractorExtraIndexField.isEditable = false
        contractorCombo.selectionModel.selectedItemProperty().addListener { _, _, newContractor ->
            val extra = extractContractorIndex(newContractor)
            contractorExtraIndexField.text = extra ?: ""
        }
    }

    private fun extractContractorIndex(obj: Any?): String? {
        if (obj == null) return null
        try {
            val candidates = listOf(
                "getExtraIndex", "getExtra", "getIndex", "getCode", "getId", "getNumber", "getIndexCode", "index", "extraIndex"
            )
            for (name in candidates) {
                try {
                    val method: Method? = try {
                        obj.javaClass.getMethod(name)
                    } catch (_: NoSuchMethodException) { null }
                    if (method != null) {
                        val value = method.invoke(obj) ?: continue
                        val s = value.toString().trim()
                        if (s.isNotEmpty()) return s
                    }
                } catch (_: Exception) { }
            }
            for (field in obj.javaClass.declaredFields) {
                try {
                    field.isAccessible = true
                    val value = field.get(obj) ?: continue
                    val s = value.toString().trim()
                    if (s.isNotEmpty()) return s
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        return null
    }

        /** Безопасная установка текста ошибки с отвязкой (unbind) свойств JavaFX */
    private fun markFieldError(control: Control, message: String) {
        control.style = "-fx-border-color: #ff6b6b; -fx-border-width: 2px;"
        if (control is TextField) {
            control.clear()
            control.promptTextProperty().unbind()
            control.promptText = message
        } else if (control is DatePicker) {
            control.value = null
            control.editor.promptTextProperty().unbind()
            control.editor.clear()
            control.editor.promptText = message
        }
    }

    /** Метод для сброса стилей как в CheckField.java */
    private fun resetErrorStyles(vararg controls: Control) {
        for (control in controls) {
            control.style = ""
            if (control is TextField) {
                control.promptTextProperty().unbind()
                control.promptText = ""
            } else if (control is DatePicker) {
                control.editor.promptTextProperty().unbind()
                control.editor.promptText = "ГГГГ-ММ-ДД"
            }
        }
    }

    private fun validateInputs(): Boolean {
        var allValid = true

        resetErrorStyles(
            plannedXField, plannedYField, plannedZField,
            actualXField, actualYField, actualZField,
            depthField, casingField, coreRecoveryField,
            startDatePicker, endDatePicker, numberField
        )

        errorLabel.text = ""

        // Вспомогательная функция для числовых полей
        fun validateNumber(field: TextField, fieldName: String): Double? {
            val txt = field.text?.trim().orEmpty()
            if (txt.isEmpty()) return null
            val d = txt.toDoubleOrNull()
            if (d == null) {
                markFieldError(field, "$fieldName должно быть числом")
                allValid = false
            }
            return d
        }

        if (numberField.text.trim().isEmpty()) {
            markFieldError(numberField, "Номер обязателен")
            allValid = false
        }

        validateNumber(plannedXField, "План X")
        validateNumber(plannedYField, "План Y")
        validateNumber(plannedZField, "План Z")
        validateNumber(actualXField, "Факт X")
        validateNumber(actualYField, "Факт Y")
        validateNumber(actualZField, "Факт Z")

        val depth = validateNumber(depthField, "Глубина")
        if (depth != null && depth < 0) {
            markFieldError(depthField, "Глубина >= 0")
            allValid = false
        }

        validateNumber(casingField, "Обсад") // Проверяем как число, сохраняем как текст

        val core = validateNumber(coreRecoveryField, "Керн")
        if (core != null && (core < 0 || core > 100)) {
            markFieldError(coreRecoveryField, "Керн от 0 до 100")
            allValid = false
        }

        // Проверка дат
        val start = startDatePicker.value
        val end = endDatePicker.value
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                markFieldError(endDatePicker, "Окончание не может быть раньше начала")
                allValid = false
            }
        }

        return allValid
    }

    @FXML
    fun onSave() {
        if (!validateInputs()) {
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
            additionalInfo = additionalInfoArea.text.ifBlank { null },
            coreRecovery = coreRecoveryField.text.toDoubleOrNull(),
            casing = casingField.text.ifBlank { null },
            closureStage = null,
            mmg1Top = mmg1TopField.text.toDoubleOrNull(),
            mmg1Bottom = mmg1BottomField.text.toDoubleOrNull(),
            mmg2Top = mmg2TopField.text.toDoubleOrNull(),
            mmg2Bottom = mmg2BottomField.text.toDoubleOrNull(),
            gwAppearLog = gwAppearLogField.text.toDoubleOrNull(),
            gwStableLog = gwStableLogField.text.toDoubleOrNull(),
            gwStableAbs = gwStableAbsField.text.toDoubleOrNull(),
            gwStableRel = gwStableRelField.text.toDoubleOrNull(),
            gwStableAbsFinal = gwStableAbsFinalField.text.toDoubleOrNull(),
            contractorExtraIndex = contractorExtraIndexField.text.ifBlank { null },
            act = actField.text.ifBlank { null },
            actNumber = actNumberField.text.ifBlank { null },
            thermalTube = thermalTubeField.text.ifBlank { null }
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

    private fun <T> ComboBox<T>.setupNameConverter(extractor: (T) -> String) {
        converter = object : javafx.util.StringConverter<T>() {
            override fun toString(obj: T?): String = obj?.let(extractor) ?: ""
            override fun fromString(string: String): T? {
                return items.find { extractor(it) == string }
            }
        }
    }
}