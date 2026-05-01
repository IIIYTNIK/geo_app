package org.example.geoapp.controller

import com.example.geoapp.api.report.ReportGenerateRequest
import com.example.geoapp.api.report.ReportOptionDto
import com.example.geoapp.api.report.ReportParameterDto
import com.example.geoapp.api.report.ReportParameterType
import com.example.geoapp.api.report.ReportTemplateDto
import com.example.geoapp.api.report.ReportTemplateMetadataDto
import com.example.geoapp.api.report.ReportTemplateSummaryDto
import com.example.geoapp.api.report.ReportValueType
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.geoapp.MainApp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import java.io.File


class ReportDialogController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var token: String = ""
    private var currentTemplate: ReportTemplateSummaryDto? = null
    private var currentParameters: List<ReportParameterDto> = emptyList()
    private val parameterControls: MutableMap<String, Node> = linkedMapOf()
    
    @FXML private lateinit var comboTemplate: ComboBox<ReportTemplateSummaryDto>
    @FXML private lateinit var comboFormat: ComboBox<String>
    @FXML private lateinit var paramsBox: VBox
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var labelDescription: Label

    @FXML
    fun initialize() {
        comboFormat.items = FXCollections.observableArrayList("PDF", "XLSX", "DOCX")
        comboFormat.selectionModel.select("PDF")

        comboTemplate.converter = object : StringConverter<ReportTemplateSummaryDto>() {
            override fun toString(obj: ReportTemplateSummaryDto?): String {
                return obj?.let { baseTemplateName(it.name) } ?: ""
            }

            override fun fromString(string: String?): ReportTemplateSummaryDto? {
                val value = string ?: return null
                return comboTemplate.items.firstOrNull { baseTemplateName(it.name) == value }
            }
        }

        comboTemplate.valueProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                labelDescription.text = newValue.description ?: "Описание отсутствует"
                loadTemplateMetadata(newValue.id)
            } else {
                labelDescription.text = ""
                clearParameters()
            }
        }
    }

    private fun loadTemplateDescription(id: Long) {
        scope.launch {
            try {
                val response = MainApp.api.getTemplate(authHeader(), id).execute()
                if (response.isSuccessful) {
                    val fullDto = response.body()
                    Platform.runLater {
                        labelDescription.text = fullDto?.description ?: "Описание отсутствует"
                    }
                }
            } catch (e: Exception) {
                Platform.runLater { labelDescription.text = "Не удалось загрузить описание" }
            }
        }
    }

    fun initData(token: String) {
        this.token = token
        loadTemplates()
    }

    @FXML
    private fun onRefreshTemplatesClick() {
        loadTemplates()
    }

    @FXML
    private fun onGenerateClick() {
        val template = comboTemplate.value ?: run {
            showError("Выберите шаблон отчёта")
            return
        }

        val format = comboFormat.value ?: "PDF"

        val params = try {
            collectParameters()
        } catch (e: Exception) {
            showError(e.message ?: "Некорректные параметры отчёта")
            return
        }

        val chooser = FileChooser().apply {
            title = "Сохранить отчёт"
            extensionFilters.add(
                ExtensionFilter(
                    "${format.uppercase()} files",
                    "*.${format.lowercase()}"
                )
            )
            initialFileName = "${baseTemplateName(template.name)}.${format.lowercase()}"
        }

        val targetFile = chooser.showSaveDialog(paramsBox.scene.window) ?: return

        setStatus("Формирование отчёта...")

        scope.launch {
            try {
                val request = ReportGenerateRequest(
                    templateId = template.id,
                    format = format,
                    params = params
                )

                val response = MainApp.api.generateReport(authHeader(), request).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    throw RuntimeException(errorBody ?: "HTTP ${response.code()}")
                }

                val body = response.body() ?: throw RuntimeException("Пустой ответ от сервера")
                val bytes = body.bytes()
                targetFile.writeBytes(bytes)

                Platform.runLater {
                    setStatus("Готово: ${targetFile.absolutePath}")
                    Alert(Alert.AlertType.INFORMATION, "Отчёт сохранён:\n${targetFile.absolutePath}")
                        .showAndWait()
                }
            } catch (e: Exception) {
                Platform.runLater {
                    showError("Ошибка генерации отчёта: ${e.message}")
                }
            }
        }
    }

    private fun loadTemplates() {
        setStatus("Загрузка шаблонов...")
        scope.launch {
            try {
                val response = MainApp.api.getReportTemplates(authHeader()).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    throw RuntimeException(errorBody ?: "HTTP ${response.code()}")
                }

                val templates = response.body().orEmpty()

                Platform.runLater {
                    comboTemplate.items = FXCollections.observableArrayList(templates)
                    if (templates.isNotEmpty()) {
                        comboTemplate.selectionModel.selectFirst()
                    } else {
                        clearParameters()
                        setStatus("Шаблоны не найдены")
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    showError("Не удалось загрузить шаблоны: ${e.message}")
                }
            }
        }
    }

    private fun loadTemplateMetadata(templateId: Long) {
        setStatus("Загрузка параметров шаблона...")
        scope.launch {
            try {
                val response = MainApp.api.getReportTemplateMetadata(authHeader(), templateId).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    throw RuntimeException(errorBody ?: "HTTP ${response.code()}")
                }

                val metadata = response.body() ?: ReportTemplateMetadataDto()

                Platform.runLater {
                    currentTemplate = comboTemplate.value
                    currentParameters = metadata.parameters
                    renderParameters(metadata.parameters)
                    setStatus("Шаблон загружен: ${currentTemplate?.name ?: ""}")
                }
            } catch (e: Exception) {
                Platform.runLater {
                    clearParameters()
                    showError("Не удалось загрузить метаданные: ${e.message}")
                }
            }
        }
    }

    private fun renderParameters(parameters: List<ReportParameterDto>) {
        paramsBox.children.clear()
        parameterControls.clear()

        if (parameters.isEmpty()) {
            paramsBox.children.add(Label("У этого шаблона нет параметров"))
            return
        }

        val grid = GridPane().apply {
            hgap = 12.0
            vgap = 10.0
            padding = Insets(10.0)
        }

        parameters.forEachIndexed { row, parameter ->
            val label = Label(buildParameterLabel(parameter))
            val control = createControl(parameter)
            parameterControls[parameter.name] = control

            grid.add(label, 0, row)
            grid.add(control, 1, row)
        }

        paramsBox.children.add(grid)
    }

    private data class MultiSelectState(
        val parameter: ReportParameterDto,
        val container: VBox,
        val rows: MutableList<HBox> = mutableListOf()
    )

    private val multiSelectStates = mutableMapOf<String, MultiSelectState>()

    private fun isMultiReferenceParameter(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith("ids")
    }

    private fun isSingleReferenceParameter(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith("id") && !n.endsWith("ids")
    }

    private fun referenceKey(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("area") -> "area"
            n.contains("contractor") -> "contractor"
            n.contains("geologist") -> "geologist"
            n.contains("drillingrig") -> "drillingrig"
            n.contains("worktype") -> "worktype"
            else -> ""
        }
    }

    private fun createSingleSelectControl(parameter: ReportParameterDto): ComboBox<ReportOptionDto> {
        return ComboBox<ReportOptionDto>().apply {
            prefWidth = 260.0

            scope.launch {
                try {
                    val options = loadOptionsFor(parameter)
                    Platform.runLater {
                        items = FXCollections.observableArrayList(options)
                        if (options.isNotEmpty()) selectionModel.selectFirst()
                    }
                } catch (e: Exception) {
                    Platform.runLater { showError("Ошибка загрузки списка: ${parameter.label}") }
                }
            }

            converter = object : StringConverter<ReportOptionDto>() {
                override fun toString(obj: ReportOptionDto?): String = obj?.label ?: ""
                override fun fromString(string: String?): ReportOptionDto? = null
            }
        }
    }

    private fun createMultiSelectControl(parameter: ReportParameterDto): VBox {
        val container = VBox(8.0)
        val state = MultiSelectState(parameter, container)
        multiSelectStates[parameter.name] = state

        val addButton = javafx.scene.control.Button("+").apply {
            setOnAction {
                addMultiSelectRow(state)
            }
        }

        container.children.add(addButton)

        scope.launch {
            try {
                val options = loadOptionsFor(parameter)
                Platform.runLater {
                    if (options.isNotEmpty()) {
                        addMultiSelectRow(state, options)
                    } else {
                        container.children.clear()
                        container.children.add(Label("Нет доступных значений"))
                    }
                }
            } catch (e: Exception) {
                Platform.runLater { showError("Ошибка загрузки списка: ${parameter.label}") }
            }
        }

        return container
    }

    private fun addMultiSelectRow(
        state: MultiSelectState,
        sourceOptions: List<ReportOptionDto> = loadOptionsFor(state.parameter)
    ) {
        val combo = ComboBox<ReportOptionDto>().apply {
            prefWidth = 260.0
            converter = object : StringConverter<ReportOptionDto>() {
                override fun toString(obj: ReportOptionDto?): String = obj?.label ?: ""
                override fun fromString(string: String?): ReportOptionDto? = null
            }

            valueProperty().addListener { _, _, _ ->
                refreshMultiSelectOptions(state)
            }
        }

        val removeButton = javafx.scene.control.Button("−").apply {
            setOnAction {
                state.container.children.removeIf { it === this@apply.parent }
                state.rows.removeIf { it.children.contains(combo) }
                refreshMultiSelectOptions(state)
            }
        }

        val row = HBox(8.0, combo, removeButton)
        state.rows.add(row)
        state.container.children.add(state.container.children.size - 1, row)

        refreshMultiSelectOptions(state, sourceOptions)
    }

    private fun refreshMultiSelectOptions(
        state: MultiSelectState,
        sourceOptions: List<ReportOptionDto> = loadOptionsFor(state.parameter)
    ) {
        val selected = state.rows.mapNotNull { row ->
            val combo = row.children.filterIsInstance<ComboBox<*>>().firstOrNull() as? ComboBox<ReportOptionDto>
            combo?.value?.value
        }.toSet()

        state.rows.forEach { row ->
            val combo = row.children.filterIsInstance<ComboBox<*>>().firstOrNull() as? ComboBox<ReportOptionDto>
                ?: return@forEach

            val current = combo.value?.value
            val filtered = sourceOptions.filter { option ->
                option.value == current || option.value !in selected
            }

            combo.items = FXCollections.observableArrayList(filtered)
            if (current != null && combo.items.none { it.value == current }) {
                combo.value = null
            }
        }
    }

    private fun createControl(parameter: ReportParameterDto): Node {
        val name = parameter.name.lowercase()

        if (isMultiReferenceParameter(name)) {
            return createMultiSelectControl(parameter)
        }

        if (isSingleReferenceParameter(name)) {
            return createSingleSelectControl(parameter)
        }

        return when (parameter.type) {
            ReportParameterType.DATE -> DatePicker().apply {
                promptText = "dd.MM.yyyy"
                prefWidth = 260.0
            }

            ReportParameterType.TEXT -> TextField().apply {
                promptText = parameter.label
                prefWidth = 260.0
            }

            ReportParameterType.NUMBER -> TextField().apply {
                promptText = "Число"
                prefWidth = 260.0
            }

            ReportParameterType.SELECT -> createSingleSelectControl(parameter)

            ReportParameterType.BOOLEAN -> CheckBox()
        }
    }

    private fun collectParameters(): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()

        currentParameters.forEach { parameter ->
            val control = parameterControls[parameter.name]
                ?: throw IllegalStateException("Не найдено поле для параметра ${parameter.name}")

            when {
                control is VBox && isMultiReferenceParameter(parameter.name) -> {
                    val state = multiSelectStates[parameter.name]
                        ?: throw IllegalStateException("Не найдено состояние для ${parameter.name}")

                    val selected = state.rows.mapNotNull { row ->
                        val combo = row.children.filterIsInstance<ComboBox<*>>().firstOrNull() as? ComboBox<ReportOptionDto>
                        combo?.value
                    }

                    if (parameter.required && selected.isEmpty()) {
                        throw IllegalArgumentException("Поле '${parameter.label}' обязательно")
                    }

                    result[parameter.name] = selected.map {
                        it.value.toLongOrNull()
                            ?: throw IllegalArgumentException("Некорректный ID в '${parameter.label}'")
                    }
                    result[parameter.name.removeSuffix("Ids") + "Names"] =
                        selected.joinToString(", ") { it.label }
                }

                control is ComboBox<*> -> {
                    val option = control.value as? ReportOptionDto
                    if (parameter.required && option == null) {
                        throw IllegalArgumentException("Поле '${parameter.label}' обязательно")
                    }

                    if (option != null) {
                        result[parameter.name] = convertTypedValue(option.value, parameter.valueType)
                        result[parameter.name.removeSuffix("Id") + "Name"] = option.label
                    } else {
                        result[parameter.name] = null
                    }
                }

                control is DatePicker -> {
                    result[parameter.name] = control.value?.toString()
                }

                control is TextField -> {
                    result[parameter.name] = parseTextValue(control.text, parameter)
                }

                control is CheckBox -> {
                    result[parameter.name] = control.isSelected
                }

                else -> {
                    result[parameter.name] = null
                }
            }
        }

        return result
    }

    private fun parseTextValue(text: String?, parameter: ReportParameterDto): Any? {
        val value = text?.trim().orEmpty()
        if (value.isBlank()) return null

        return when (parameter.valueType) {
            ReportValueType.STRING -> value
            ReportValueType.NUMBER -> value.toLongOrNull()
                ?: value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Параметр '${parameter.label}' должен быть числом")
            ReportValueType.DECIMAL -> value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Параметр '${parameter.label}' должен быть десятичным числом")
            ReportValueType.BOOLEAN -> value.toBooleanStrictOrNull()
                ?: throw IllegalArgumentException("Параметр '${parameter.label}' должен быть true/false")
        }
    }

    private fun convertTypedValue(raw: String, type: ReportValueType): Any? {
        return when (type) {
            ReportValueType.STRING -> raw
            ReportValueType.NUMBER -> raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw
            ReportValueType.DECIMAL -> raw.toDoubleOrNull() ?: raw
            ReportValueType.BOOLEAN -> raw.toBooleanStrictOrNull() ?: raw
        }
    }

    private fun buildParameterLabel(parameter: ReportParameterDto): String {
        return if (parameter.required) "${parameter.label} *" else parameter.label
    }

    private fun clearParameters() {
        paramsBox.children.clear()
        parameterControls.clear()
        currentParameters = emptyList()
        currentTemplate = null
    }

    private fun authHeader(): String = if (token.startsWith("Bearer ")) token else "Bearer $token"

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "report" }
    }

    private fun setStatus(text: String) {
        statusLabel.text = text
    }

    private fun showError(message: String) {
        setStatus(message)
        Alert(Alert.AlertType.ERROR, message).showAndWait()
    }

    @FXML
    private fun onUploadTemplate() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(ExtensionFilter("Jasper Reports", "*.jrxml"))
        val file = fileChooser.showOpenDialog(paramsBox.scene.window) ?: return

        val descriptionInput = TextInputDialog().apply {
            title = "Загрузка шаблона"
            headerText = "Введите описание для шаблона \"${file.name}\""
            contentText = "Описание:"
        }.showAndWait().orElse("")

        performUpload(file, descriptionInput, false)
    }

    private fun performUpload(file: File, description: String, overwrite: Boolean) {
        scope.launch {
            try {
                val mediaTypePlain = "text/plain".toMediaTypeOrNull()
                val mediaTypeXml = "application/xml".toMediaTypeOrNull()

                val namePart = file.name.toRequestBody(mediaTypePlain)
                val descPart = description.toRequestBody(mediaTypePlain)
                val overwritePart = overwrite.toString().toRequestBody(mediaTypePlain)
                
                val filePart = MultipartBody.Part.createFormData(
                    "file", 
                    file.name, 
                    file.readBytes().toRequestBody(mediaTypeXml)
                )

                val response = MainApp.api.uploadTemplate(
                    authHeader(), 
                    namePart, 
                    descPart, 
                    filePart, 
                    overwritePart
                ).execute()

                when {
                    response.isSuccessful -> {
                        Platform.runLater {
                            Alert(Alert.AlertType.INFORMATION, "Успешно сохранено").show()
                            loadTemplates()
                        }
                    }
                    response.code() == 409 -> {
                        Platform.runLater {
                            val confirm = Alert(Alert.AlertType.CONFIRMATION,
                                "Шаблон с таким именем уже существует. Перезаписать?").showAndWait()
                            if (confirm.isPresent && confirm.get() == ButtonType.OK) {
                                performUpload(file, description, true)
                            }
                        }
                    }
                    else -> throw RuntimeException("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                Platform.runLater { showError("Ошибка: ${e.message}") }
            }
        }
    }

    private fun baseTemplateName(name: String): String {
        return name.trim()
            .replace(Regex("\\.(jrxml|jasper)$", RegexOption.IGNORE_CASE), "")
    }

    
    // Метод для удаления выбранного шаблона
    @FXML
    private fun onDeleteTemplate() {
        val selected = comboTemplate.value ?: return
        
        val confirm = Alert(Alert.AlertType.CONFIRMATION, "Удалить шаблон '${selected.name}'?").showAndWait()
        if (confirm.get() != ButtonType.OK) return

        scope.launch {
            try {
                val response = MainApp.api.deleteTemplate(authHeader(), selected.id).execute()
                if (response.isSuccessful) {
                    Platform.runLater {
                        Alert(Alert.AlertType.INFORMATION, "Шаблон удален").show()
                        loadTemplates() // Перезагружаем список
                    }
                } else {
                    showError("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                Platform.runLater { showError("Не удалось удалить: ${e.message}") }
            }
        }
    }

    // Метод для скачивания (извлечения) шаблона с сервера
    @FXML
    private fun onDownloadTemplate() {
        val selected = comboTemplate.value ?: return
        
        val fileChooser = FileChooser()
        fileChooser.initialFileName = "${selected.name}.jrxml"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Jasper Reports", "*.jrxml"))
        val file = fileChooser.showSaveDialog(comboTemplate.scene.window) ?: return

        scope.launch {
            try {
                val response = MainApp.api.getTemplate(authHeader(), selected.id).execute()
                if (response.isSuccessful) {
                    val dto = response.body()
                    dto?.jrxmlContent?.let { content ->
                        file.writeText(content)
                        Platform.runLater { Alert(Alert.AlertType.INFORMATION, "Шаблон сохранен в ${file.name}").show() }
                    }
                }
            } catch (e: Exception) {
                Platform.runLater { showError("Ошибка при скачивании: ${e.message}") }
            }
        }
    }

    private fun loadOptionsFor(parameter: ReportParameterDto): List<ReportOptionDto> {
        val name = parameter.name.lowercase()

        return when {
            name.contains("area") -> MainApp.api.getAreas().execute().body()
                ?.map { ReportOptionDto(it.id.toString(), it.name) } ?: emptyList()

            name.contains("contractor") -> MainApp.api.getContractors().execute().body()
                ?.map { ReportOptionDto(it.id.toString(), it.name) } ?: emptyList()

            name.contains("geologist") -> MainApp.api.getGeologists().execute().body()
                ?.map { ReportOptionDto(it.id.toString(), it.name) } ?: emptyList()

            name.contains("drillingrig") -> MainApp.api.getDrillingRigs().execute().body()
                ?.map { ReportOptionDto(it.id.toString(), it.name) } ?: emptyList()

            name.contains("worktype") -> MainApp.api.getWorkTypes().execute().body()
                ?.map { ReportOptionDto(it.id.toString(), it.name) } ?: emptyList()

            else -> emptyList()
        }
    }

}