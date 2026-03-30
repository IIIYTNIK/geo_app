package org.example.geoapp.controller

import com.example.geoapp.api.*
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.StringConverter
import java.time.LocalDate
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.NumericFieldUtil
import org.example.geoapp.util.NumberParsers.toDoubleSafe

class WorkingFormController {

    @FXML private lateinit var root: VBox
    @FXML private lateinit var areaCombo: ComboBox<RefArea>
    @FXML private lateinit var workTypeCombo: ComboBox<RefWorkType>
    @FXML private lateinit var geologistCombo: ComboBox<RefGeologist>
    @FXML private lateinit var contractorCombo: ComboBox<RefContractor>
    @FXML private lateinit var drillingRigCombo: ComboBox<RefDrillingRig>
    
    @FXML private lateinit var numberField: TextField
    @FXML private lateinit var plannedXField: TextField
    @FXML private lateinit var plannedYField: TextField
    @FXML private lateinit var actualXField: TextField
    @FXML private lateinit var actualYField: TextField
    @FXML private lateinit var actualZField: TextField
    @FXML private lateinit var deltaSLabel: Label

    @FXML private lateinit var depthField: TextField
    @FXML private lateinit var casingField: TextField
    @FXML private lateinit var coreRecoveryField: TextField
    
    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker
    @FXML private lateinit var additionalInfoArea: TextArea

    @FXML private lateinit var mmg1TopField: TextField
    @FXML private lateinit var mmg1BottomField: TextField
    @FXML private lateinit var mmg2TopField: TextField
    @FXML private lateinit var mmg2BottomField: TextField
    
    @FXML private lateinit var gwAppearLogField: TextField
    @FXML private lateinit var gwStableLogField: TextField
    @FXML private lateinit var gwStableAbsLabel: Label

    @FXML private lateinit var actCheckBox: CheckBox
    @FXML private lateinit var actNumberField: TextField
    @FXML private lateinit var thermalTubeCheckBox: CheckBox

    @FXML private lateinit var saveButton: Button
    @FXML private lateinit var cancelButton: Button
    @FXML private lateinit var errorLabel: Label

    private lateinit var token: String
    private var working: Working? = null
    private var onSaveCallback: () -> Unit = {}
    private val api = MainApp.api

    @FXML
    fun initialize() {
        actNumberField.disableProperty().bind(actCheckBox.selectedProperty().not())
        
        // Настраиваем конвертеры сразу при инициализации окна
        setupAllConverters()
        setupNumericFields()

        root.sceneProperty().addListener { _, _, newScene ->
            newScene?.accelerators?.put(KeyCodeCombination(KeyCode.ESCAPE)) { onCancel() }
        }
    }

    fun initData(token: String, working: Working?, onSave: () -> Unit) {
        this.token = token
        this.working = working
        this.onSaveCallback = onSave

        // 1. Сначала загружаем общие справочники
        loadReferences {
            // 2. После того как списки загружены, заполняем поля (если это редактирование)
            if (working != null) {
                fillFields()
            }
        }

        // Слушатель для фильтрации геологов по подрядчику
        contractorCombo.selectionModel.selectedItemProperty().addListener { _, _, newContractor ->
            loadGeologistsForContractor(newContractor?.id)
        }
    }

    private fun setupAllConverters() {
        // Используем расширение для всех комбобоксов
        areaCombo.setupNameConverter { it.name }
        workTypeCombo.setupNameConverter { it.name }
        contractorCombo.setupNameConverter { it.name }
        geologistCombo.setupNameConverter { it.name }
        drillingRigCombo.setupNameConverter { it.name }
    }

    private fun loadReferences(onReady: () -> Unit) {
        runOnFx {
            try {
                // Загружаем данные с сервера
                val areas = api.getAreas().await()
                val types = api.getWorkTypes().await()
                val contractors = api.getContractors().await()
                val rigs = api.getDrillingRigs().await()

                // Обновляем списки
                areaCombo.items.setAll(areas)
                workTypeCombo.items.setAll(types)
                contractorCombo.items.setAll(contractors)
                drillingRigCombo.items.setAll(rigs)

                onReady()
            } catch (e: Exception) {
                errorLabel.text = "Ошибка загрузки справочников"
            }
        }
    }

    private fun fillFields() {
        val w = working ?: return

        numberField.text = w.number ?: ""
        plannedXField.text = w.plannedX?.toString() ?: ""
        plannedYField.text = w.plannedY?.toString() ?: ""
        actualXField.text = w.actualX?.toString() ?: ""
        actualYField.text = w.actualY?.toString() ?: ""
        actualZField.text = w.actualZ?.toString() ?: ""
        
        //deltaSLabel.text = "Смещение от проекта: ${w.deltaS ?: "не определено"} м"

        depthField.text = w.depth?.toString() ?: ""
        casingField.text = w.casing?.toString() ?: ""
        coreRecoveryField.text = w.coreRecovery?.toString() ?: ""

        startDatePicker.value = w.startDate?.let { LocalDate.parse(it) }
        endDatePicker.value = w.endDate?.let { LocalDate.parse(it) }

        additionalInfoArea.text = w.additionalInfo ?: ""

        mmg1TopField.text = w.mmg1Top?.toString() ?: ""
        mmg1BottomField.text = w.mmg1Bottom?.toString() ?: ""
        mmg2TopField.text = w.mmg2Top?.toString() ?: ""
        mmg2BottomField.text = w.mmg2Bottom?.toString() ?: ""

        gwAppearLogField.text = w.gwAppearLog?.toString() ?: ""
        gwStableLogField.text = w.gwStableLog?.toString() ?: ""
        gwStableAbsLabel.text = "УУГВ абс: ${w.gwStableAbs ?: "не определено"}"

        actCheckBox.isSelected = w.act == true
        actNumberField.text = w.actNumber ?: ""
        thermalTubeCheckBox.isSelected = w.thermalTube == true

        // Важно: выбираем объекты из загруженных списков по ID
        areaCombo.items.find { it.id == w.area?.id }?.let { areaCombo.value = it }
        workTypeCombo.items.find { it.id == w.workType?.id }?.let { workTypeCombo.value = it }
        contractorCombo.items.find { it.id == w.contractor?.id }?.let { contractorCombo.value = it }
        drillingRigCombo.items.find { it.id == w.drillingRig?.id }?.let { drillingRigCombo.value = it }

        // Если есть подрядчик, грузим его геологов
        w.contractor?.id?.let { loadGeologistsForContractor(it, w.geologist?.id) }
    }

    private fun loadGeologistsForContractor(contractorId: Long?, selectedGeologistId: Long? = null) {
        if (contractorId == null) {
            geologistCombo.items.clear()
            return
        }
        runOnFx {
            try {
                val geologists = api.getGeologistsByContractor("Bearer $token", contractorId).await()
                geologistCombo.items.setAll(geologists)
                
                // Если мы передали ID (при первичной загрузке), выбираем его
                if (selectedGeologistId != null) {
                    geologistCombo.items.find { it.id == selectedGeologistId }?.let { 
                        geologistCombo.value = it 
                    }
                }
            } catch (e: Exception) {
                geologistCombo.items.clear()
            }
        }
    }

    // Универсальное расширение для настройки отображения
    private fun <T> ComboBox<T>.setupNameConverter(extractor: (T) -> String) {
        this.converter = object : StringConverter<T>() {
            override fun toString(obj: T?): String = obj?.let(extractor) ?: ""
            override fun fromString(string: String?): T? {
                return items.find { extractor(it) == string }
            }
        }
    }

    private fun setupNumericFields() {
        listOf(
            plannedXField, plannedYField, actualXField, actualYField, actualZField, 
            depthField, casingField, mmg1TopField, mmg1BottomField, mmg2TopField, 
            mmg2BottomField, gwAppearLogField, gwStableLogField, coreRecoveryField
        ).forEach { NumericFieldUtil.applyDecimalFilter(it) }

        listOf(plannedXField, plannedYField, actualXField, actualYField).forEach { field ->
            field.textProperty().addListener { _, _, _ -> updateDeltaS() }
        }
    }

    private fun updateDeltaS() {
        val plannedX = plannedXField.toDoubleSafe()
        val plannedY = plannedYField.toDoubleSafe()
        val actualX = actualXField.toDoubleSafe()
        val actualY = actualYField.toDoubleSafe()

        val deltaS = if (plannedX != null && plannedY != null && actualX != null && actualY != null) {
            val dx = plannedX - actualX
            val dy = plannedY - actualY
            val distance = Math.sqrt(dx * dx + dy * dy)
            "%.3f".format(distance)
        } else {
            "не определено"
        }
        deltaSLabel.text = "Смещение от проекта: $deltaS м"
    }

    @FXML fun onCancel() = close()
    private fun close() = (root.scene.window as Stage).close()

    @FXML fun onSave() {
        if (!validateInputs()) return

        val newWorking = Working(
            id = working?.id ?: 0,
            area = areaCombo.value,
            workType = workTypeCombo.value,
            number = numberField.text.trim(),
            plannedX = plannedXField.toDoubleSafe(),
            plannedY = plannedYField.toDoubleSafe(),
            actualX = actualXField.toDoubleSafe(),
            actualY = actualYField.toDoubleSafe(),
            actualZ = actualZField.toDoubleSafe(),
            depth = depthField.toDoubleSafe(),
            startDate = startDatePicker.value?.toString(),
            endDate = endDatePicker.value?.toString(),
            geologist = geologistCombo.value,
            contractor = contractorCombo.value,
            drillingRig = drillingRigCombo.value,
            additionalInfo = additionalInfoArea.text.ifBlank { null },
            coreRecovery = coreRecoveryField.toDoubleSafe(),
            casing = casingField.toDoubleSafe(),
            mmg1Top = mmg1TopField.toDoubleSafe(),
            mmg1Bottom = mmg1BottomField.toDoubleSafe(),
            mmg2Top = mmg2TopField.toDoubleSafe(),
            mmg2Bottom = mmg2BottomField.toDoubleSafe(),
            gwAppearLog = gwAppearLogField.toDoubleSafe(),
            gwStableLog = gwStableLogField.toDoubleSafe(),
            act = actCheckBox.isSelected,
            actNumber = if (actCheckBox.isSelected) actNumberField.text.ifBlank { null } else null,
            thermalTube = thermalTubeCheckBox.isSelected,
            hasVideo = working?.hasVideo ?: false,
            hasDrilling = working?.hasDrilling ?: false,
            hasJournal = working?.hasJournal ?: false,
            hasCore = working?.hasCore ?: false,
            hasRod = working?.hasRod ?: false,
            isProject = working?.isProject ?: false
        )

        saveButton.isDisable = true
        runOnFx {
            try {
                if (working == null) api.createWorking("Bearer $token", newWorking).await()
                else api.updateWorking("Bearer $token", working!!.id, newWorking).await()
                onSaveCallback()
                close()
            } catch (e: Exception) {
                errorLabel.text = "Ошибка: ${e.message}"
                saveButton.isDisable = false
            }
        }
    }

    private fun validateInputs(): Boolean {
        var allValid = true
        errorLabel.text = ""
        // Упрощенная валидация для краткости
        if (numberField.text.isNullOrBlank()) {
            numberField.style = "-fx-border-color: red"
            allValid = false
        }
        return allValid
    }
}