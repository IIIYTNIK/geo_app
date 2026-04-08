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
    @FXML private lateinit var plannedDepthField: TextField
    @FXML private lateinit var actualXField: TextField
    @FXML private lateinit var actualYField: TextField
    @FXML private lateinit var actualZField: TextField
    @FXML private lateinit var actualDepthField: TextField
    @FXML private lateinit var deltaSLabel: Label
    
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

    private var updatingContractor = false

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
            if (!updatingContractor) {
                val currentGeologistId = geologistCombo.value?.id
                loadGeologistsForContractor(newContractor?.id, currentGeologistId)
            }
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
        plannedXField.text = w.plannedX?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        plannedYField.text = w.plannedY?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        actualXField.text = w.actualX?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        actualYField.text = w.actualY?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        actualZField.text = w.actualZ?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        
        deltaSLabel.text = "Смещение от проекта: ${w.deltaS?.let { "%.3f".format(it).replace(",", ".") } ?: "не определено"} м"

        plannedDepthField.text = w.plannedDepth?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        actualDepthField.text = w.actualDepth?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        casingField.text = w.casing?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        coreRecoveryField.text = w.coreRecovery?.let { "%.3f".format(it).replace(",", ".") } ?: ""

        startDatePicker.value = w.startDate?.let { LocalDate.parse(it) }
        endDatePicker.value = w.endDate?.let { LocalDate.parse(it) }

        additionalInfoArea.text = w.additionalInfo ?: ""

        mmg1TopField.text = w.mmg1Top?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        mmg1BottomField.text = w.mmg1Bottom?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        mmg2TopField.text = w.mmg2Top?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        mmg2BottomField.text = w.mmg2Bottom?.let { "%.3f".format(it).replace(",", ".") } ?: ""

        gwAppearLogField.text = w.gwAppearLog?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        gwStableLogField.text = w.gwStableLog?.let { "%.3f".format(it).replace(",", ".") } ?: ""
        gwStableAbsLabel.text = "УУГВ абс: ${w.gwStableAbs?.let { "%.3f".format(it).replace(",", ".") } ?: "не определено"}"

        actCheckBox.isSelected = w.act == true
        actNumberField.text = w.actNumber ?: ""
        thermalTubeCheckBox.isSelected = w.thermalTube == true

        // Важно: выбираем объекты из загруженных списков по ID
        areaCombo.items.find { it.id == w.area?.id }?.let { areaCombo.value = it }
        workTypeCombo.items.find { it.id == w.workType?.id }?.let { workTypeCombo.value = it }
        //contractorCombo.items.find { it.id == w.contractor?.id }?.let { contractorCombo.value = it }
        updatingContractor = true
        contractorCombo.items.find { it.id == w.contractor?.id }?.let { contractorCombo.value = it }
        updatingContractor = false
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
                if (selectedGeologistId != null) {
                    val found = geologists.find { it.id == selectedGeologistId }
                    println("Looking for geologist id $selectedGeologistId, found = $found")
                    if (found != null) {
                        geologistCombo.value = found
                        println("geologistCombo.value set to $found")
                    } else {
                        println("Geologist with id $selectedGeologistId not found in loaded list. Available ids: ${geologists.map { it.id }}")
                    }
                }
            } catch (e: Exception) {
                println("Error loading geologists: ${e.message}")
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
            plannedDepthField, actualDepthField, casingField, mmg1TopField, mmg1BottomField, mmg2TopField, 
            mmg2BottomField, gwAppearLogField, gwStableLogField, coreRecoveryField
        ).forEach { NumericFieldUtil.applyDecimalFilter(it) }

        // Пересчёт смещения
        listOf(plannedXField, plannedYField, actualXField, actualYField).forEach { field ->
            field.textProperty().addListener { _, _, _ -> updateDeltaS() }
        }

        // Пересчёт абсолютной отметки УГВ
        listOf(actualZField, gwStableLogField).forEach { field ->
            field.textProperty().addListener { _, _, _ -> updateGwStableAbs() }
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

        val existing = working // текущий объект (если редактирование)
        val newWorking = (existing ?: Working(number = "")).copy(
            id = existing?.id ?: 0,
            orderNum = existing?.orderNum,
            area = areaCombo.value,
            workType = workTypeCombo.value,
            number = numberField.text.trim(),
            plannedX = plannedXField.toDoubleSafe(),
            plannedY = plannedYField.toDoubleSafe(),
            plannedDepth = plannedDepthField.toDoubleSafe(),
            actualX = actualXField.toDoubleSafe(),
            actualY = actualYField.toDoubleSafe(),
            actualZ = actualZField.toDoubleSafe(),
            actualDepth = actualDepthField.toDoubleSafe(),
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
            // Сохраняем все остальные поля из существующего объекта
            hasVideo = existing?.hasVideo ?: false,
            hasDrilling = existing?.hasDrilling ?: false,
            hasJournal = existing?.hasJournal ?: false,
            hasCore = existing?.hasCore ?: false,
            hasStake = existing?.hasStake ?: false,
            isProject = existing?.isProject ?: false,
            structure = existing?.structure,
            plannedContractor = existing?.plannedContractor,
            cat1_4 = existing?.cat1_4,
            cat5_8 = existing?.cat5_8,
            cat9_12 = existing?.cat9_12
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

    private fun Double?.toPlain(): String = this?.let { java.math.BigDecimal(it).stripTrailingZeros().toPlainString() } ?: ""

    private fun updateGwStableAbs() {
        val actualZ = actualZField.toDoubleSafe()
        val gwStableLog = gwStableLogField.toDoubleSafe()

        val absValue = if (actualZ != null && gwStableLog != null) {
            val result = actualZ - gwStableLog
            "%.3f".format(result).replace(",", ".")
        } else {
            "не определено"
        }
        gwStableAbsLabel.text = "УУГВ абс: $absValue"
    }

}