package org.example.geoapp.controller

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.Working
import org.example.geoapp.util.await
import org.example.geoapp.util.runOnFx
import java.io.File

// Вспомогательный класс для отображения строк в мастере
class MediaMappingRow(val working: Working) {

    // Вспомогательная функция для сборки полного имени
    private fun getFullWorkingName(w: Working): String {
        val prefix = when(w.workType?.name?.lowercase()) {
            "скважина" -> "С-"
            "шурф" -> "Ш-"
            "расчистка" -> "Р-"
            else -> ""
        }
        return "$prefix${w.number}"
    }

    val number = SimpleStringProperty(getFullWorkingName(working))
    val currentPath = SimpleStringProperty(working.mediaPath ?: "")
    val newPath = SimpleStringProperty("")
    val status = SimpleStringProperty("Ожидание")

    // Логика обновления статуса
    fun updateStatus(overwriteAllowed: Boolean) {
        val path = newPath.value
        if (path.isNullOrBlank()) {
            status.value = "Папка не указана"
        } else if (!working.mediaPath.isNullOrBlank() && !overwriteAllowed) {
            status.value = "Пропуск (путь уже есть)"
        } else {
            status.value = "Готово к записи"
        }
    }
}

class MediaWizardController {

    @FXML private lateinit var rootPathField: TextField
    @FXML private lateinit var mappingTable: TableView<MediaMappingRow>
    @FXML private lateinit var colNumber: TableColumn<MediaMappingRow, String>
    @FXML private lateinit var colCurrentPath: TableColumn<MediaMappingRow, String>
    @FXML private lateinit var colNewPath: TableColumn<MediaMappingRow, String>
    @FXML private lateinit var colStatus: TableColumn<MediaMappingRow, String>
    @FXML private lateinit var overwriteCheckBox: CheckBox
    @FXML private lateinit var saveButton: Button

    private lateinit var token: String
    private lateinit var workings: List<Working>
    private lateinit var onComplete: () -> Unit
    private val api: GeoApi = MainApp.api

    private val rows = FXCollections.observableArrayList<MediaMappingRow>()

    fun initData(token: String, selectedWorkings: List<Working>, onComplete: () -> Unit) {
        this.token = token
        this.workings = selectedWorkings
        this.onComplete = onComplete

        setupTable()
        
        // Превращаем выработки в строки таблицы
        rows.addAll(workings.map { MediaMappingRow(it) })
        mappingTable.items = rows

        // Если галочка меняется, обновляем статусы у всех строк
        overwriteCheckBox.selectedProperty().addListener { _, _, _ ->
            rows.forEach { it.updateStatus(overwriteCheckBox.isSelected) }
            mappingTable.refresh()
        }
        
        // Сразу проставляем начальные статусы
        rows.forEach { it.updateStatus(overwriteCheckBox.isSelected) }
    }

    private fun setupTable() {
        colNumber.setCellValueFactory { it.value.number }
        colCurrentPath.setCellValueFactory { it.value.currentPath }
        colStatus.setCellValueFactory { it.value.status }

        // Привязываем свойство newPath
        colNewPath.setCellValueFactory { it.value.newPath }
        
        // Кастомная ячейка: Текстовое поле + кнопка "..."
        colNewPath.setCellFactory {
            object : TableCell<MediaMappingRow, String>() {
                val textField = TextField().apply {
                    HBox.setHgrow(this, Priority.ALWAYS)
                    // Сохраняем ручной ввод обратно в модель
                    textProperty().addListener { _, _, newText ->
                        if (index in 0 until tableView.items.size) {
                            val row = tableView.items[index]
                            row.newPath.value = newText
                            row.updateStatus(overwriteCheckBox.isSelected)
                        }
                    }
                }
                val btn = Button("...").apply {
                    setOnAction {
                        val chooser = DirectoryChooser()
                        chooser.title = "Выберите папку для выработки"
                        val dir = chooser.showDialog(scene.window)
                        if (dir != null) {
                            textField.text = dir.absolutePath // Это вызовет слушатель выше
                        }
                    }
                }
                val hbox = HBox(5.0, textField, btn)

                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || tableRow == null) {
                        graphic = null
                    } else {
                        textField.text = item ?: ""
                        graphic = hbox
                    }
                }
            }
        }
    }

    @FXML fun onSelectRoot() {
        val chooser = DirectoryChooser()
        chooser.title = "Выберите корневую папку медиафайлов"
        val rootDir = chooser.showDialog(mappingTable.scene.window) ?: return

        rootPathField.text = rootDir.absolutePath
        autoMatchFolders(rootDir)
    }

    private fun autoMatchFolders(rootDir: File) {
        // Получаем список всех подпапок в выбранном корне
        val subDirs = rootDir.listFiles { file -> file.isDirectory } ?: emptyArray()

        for (row in rows) {
            val expectedFolderName = row.number.value
            // Ищем папку, имя которой полностью совпадает с номером скважины
            // ignoreCase = true позволяет игнорировать регистр (С-12 и с-12)
            val matchedDir = subDirs.find { it.name.equals(expectedFolderName, ignoreCase = true) }
            
            if (matchedDir != null) {
                row.newPath.value = matchedDir.absolutePath
            }
            row.updateStatus(overwriteCheckBox.isSelected)
        }
        mappingTable.refresh()
    }

    @FXML fun onSave() {
        // Отбираем только те строки, которые отмечены как "Готово к записи"
        val rowsToUpdate = rows.filter { it.status.value == "Готово к записи" && !it.newPath.value.isNullOrBlank() }

        if (rowsToUpdate.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Нет данных для сохранения.")
            return
        }

        saveButton.isDisable = true

        runOnFx {
            try {
                // Отправляем запросы на сервер по очереди 
                for (row in rowsToUpdate) {
                    val updatedWorking = row.working.copy()
                    updatedWorking.mediaPath = row.newPath.value
                    
                    api.updateWorking("Bearer $token", updatedWorking.id, updatedWorking).await()
                }

                showAlert(Alert.AlertType.INFORMATION, "Успешно обновлено ${rowsToUpdate.size} записей.")
                onComplete()
                closeWindow()
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Ошибка при сохранении: ${e.message}")
            } finally {
                saveButton.isDisable = false
            }
        }
    }

    @FXML fun onCancel() {
        closeWindow()
    }

    private fun closeWindow() {
        (mappingTable.scene.window as Stage).close()
    }

    private fun showAlert(type: Alert.AlertType, message: String) {
        Alert(type).apply {
            title = if (type == Alert.AlertType.ERROR) "Ошибка" else "Информация"
            headerText = null
            contentText = message
            showAndWait()
        }
    }
}