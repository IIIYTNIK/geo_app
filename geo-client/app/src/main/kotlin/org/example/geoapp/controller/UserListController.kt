package org.example.geoapp.controller


import org.example.geoapp.api.UserDto
import org.example.geoapp.api.AccessLevel
import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.UserCreateDto
import org.example.geoapp.api.UserUpdateDto
import org.example.geoapp.api.RefArea
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx


// Класс-обертка для отображения строки в таблице
class UserRowModel(
    val user: UserDto,
    val accessMap: MutableMap<Long, SimpleObjectProperty<AccessLevel?>> = mutableMapOf()
)

class UserListController {

    @FXML private lateinit var usersTable: TableView<UserRowModel>
    @FXML private lateinit var colId: TableColumn<UserRowModel, Long>
    @FXML private lateinit var colLogin: TableColumn<UserRowModel, String>
    @FXML private lateinit var colFullName: TableColumn<UserRowModel, String>
    @FXML private lateinit var colRole: TableColumn<UserRowModel, String>
    @FXML private lateinit var colPosition: TableColumn<UserRowModel, String>

    @FXML private lateinit var loginField: TextField
    @FXML private lateinit var fullNameField: TextField
    @FXML private lateinit var passwordField: PasswordField
    @FXML private lateinit var roleCombo: ComboBox<String>
    @FXML private lateinit var positionField: TextField
    @FXML private lateinit var errorLabel: Label

    @FXML private lateinit var deleteButton: Button
    @FXML private lateinit var clearButton: Button
    @FXML private lateinit var saveButton: Button

    private lateinit var token: String
    private val api: GeoApi = MainApp.api
    private var selectedUser: UserDto? = null

    private val dynamicColumns = mutableListOf<TableColumn<UserRowModel, String>>()

    fun initData(token: String) {
        this.token = token
        setupTable()
        setupForm()
        loadUsers()
        clearForm()
    }

    private fun setupTable() {
        colId.setCellValueFactory { SimpleObjectProperty(it.value.user.id) }
        colLogin.setCellValueFactory { SimpleStringProperty(it.value.user.login) }
        colFullName.setCellValueFactory { SimpleStringProperty(it.value.user.fullName) }
        colRole.setCellValueFactory { SimpleStringProperty(roleToRu(it.value.user.role)) }
        colPosition.setCellValueFactory { SimpleStringProperty(it.value.user.position ?: "") }

        colId.isEditable = false
        colLogin.isEditable = false
        colFullName.isEditable = false
        colRole.isEditable = false
        colPosition.isEditable = false

        usersTable.isEditable = true

        usersTable.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            selectedUser = newValue?.user
            fillForm(newValue?.user)
            updateButtons()
        }

        deleteButton.disableProperty().bind(usersTable.selectionModel.selectedItemProperty().isNull)
    }

   private fun setupForm() {
        roleCombo.items = FXCollections.observableArrayList("ROLE_USER", "ROLE_ADMIN")
        roleCombo.converter = object : javafx.util.StringConverter<String>() {
            override fun toString(obj: String?): String = roleToRu(obj ?: "")
            override fun fromString(string: String?): String? = when (string) {
                "Администратор" -> "ROLE_ADMIN"
                "Пользователь" -> "ROLE_USER"
                else -> string
            }
        }

        roleCombo.selectionModel.select("ROLE_USER")

        loginField.textProperty().addListener { _, _, _ -> updateButtons() }
        fullNameField.textProperty().addListener { _, _, _ -> updateButtons() }
        passwordField.textProperty().addListener { _, _, _ -> updateButtons() }
        positionField.textProperty().addListener { _, _, _ -> updateButtons() }
    }

    @FXML
    fun onClearSelection() {
        usersTable.selectionModel.clearSelection()
        clearForm()
    }

    @FXML
    fun onSave() {
        val login = loginField.text.trim()
        val fullName = fullNameField.text.trim()
        val role = roleCombo.value
        val password = passwordField.text

        if (login.isBlank()) {
            showAlert("Ошибка", "Введите логин")
            return
        }

        if (fullName.isBlank()) {
            showAlert("Ошибка", "Введите ФИО")
            return
        }

        if (role.isNullOrBlank()) {
            showAlert("Ошибка", "Выберите роль")
            return
        }

        val current = selectedUser
        val tokenStr = authHeader()

        runOnFx {
            try {
                if (current == null) {
                    if (password.isBlank()) {
                        showAlert("Ошибка", "Для нового пользователя нужно задать пароль")
                        return@runOnFx
                    }

                    api.createUser(
                        tokenStr,
                        UserCreateDto(login = login, fullName = fullName, password = password, role = role.removePrefix("ROLE_"), position = positionField.text.trim().ifBlank { null })
                    ).await()
                } else {
                    api.updateUser(tokenStr, current.id, UserUpdateDto(login = login, fullName = fullName, role = role, password = password.trim().ifBlank { null }, position = positionField.text.trim().ifBlank { null })).await()
                }

                loadUsers()
                usersTable.selectionModel.clearSelection()
                clearForm()
                onClearSelection()
            } catch (e: Exception) {
                showAlert("Ошибка сохранения", e.message ?: "Не удалось сохранить пользователя")
            }
        }
    }

    @FXML
    fun onDelete() {
        val selected = usersTable.selectionModel.selectedItem ?: return
        val confirm = Alert(Alert.AlertType.CONFIRMATION, "Удалить пользователя '${selected.user.fullName}'?", ButtonType.YES, ButtonType.NO)
        confirm.dialogPane.graphic = null

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return

        runOnFx {
            try {
                api.deleteUser(authHeader(), selected.user.id).awaitUnit()
                loadUsers()
                onClearSelection()
            } catch (e: Exception) {
                showAlert("Ошибка удаления", e.message ?: "Не удалось удалить пользователя")
            }
        }
    }

    private fun loadUsers() {
        runOnFx {
            try {
                val tokenStr = authHeader()
                
                // Грузим параллельно всё, что нужно для матрицы
                val users = api.getUsers(tokenStr).await().sortedBy { it.id }
                val areas = api.getAreas().await()
                val accesses = api.getAllUsersAccess(tokenStr).await()

                buildDynamicColumns(areas)

                // Собираем матрицу: Пользователь + его Права
                val rowModels = users.map { user ->
                    val row = UserRowModel(user)
                    areas.forEach { area ->
                        val currentAccess = accesses.find { it.userId == user.id && it.areaId == area.id }?.accessLevel
                        row.accessMap[area.id] = SimpleObjectProperty(currentAccess)
                    }
                    row
                }

                usersTable.items = FXCollections.observableArrayList(rowModels)
                autoSizeColumnsAndWindow()
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить данные: ${e.message}")
            }
        }
    }

    private fun buildDynamicColumns(areas: List<RefArea>) {
        usersTable.columns.removeAll(dynamicColumns)
        dynamicColumns.clear()

        val options = FXCollections.observableArrayList("Нет доступа", "Чтение", "Запись")

        for (area in areas) {
            val areaCol = TableColumn<UserRowModel, String>(area.name)
            areaCol.prefWidth = 110.0

            // Привязка: переводим enum в русскую строку
            areaCol.setCellValueFactory { cellData ->
                val access = cellData.value.accessMap[area.id]?.value
                val strValue = when (access) {
                    AccessLevel.WRITE -> "Запись"
                    AccessLevel.READ -> "Чтение"
                    else -> "Нет доступа"
                }
                SimpleStringProperty(strValue)
            }

            // Выпадающий список
            areaCol.setCellFactory(ComboBoxTableCell.forTableColumn(options))

            // Сохранение при выборе нового значения
            areaCol.setOnEditCommit { event ->
                val userRow = event.rowValue
                val newAccessLevelStr = event.newValue
                val areaId = area.id

                val backendValue = when (newAccessLevelStr) {
                    "Запись" -> "WRITE"
                    "Чтение" -> "READ"
                    else -> "NONE"
                }

                runOnFx {
                    try {
                        api.updateUserAccess(authHeader(), userRow.user.id, areaId, backendValue).awaitUnit()
                        // Обновляем локальное состояние в случае успеха
                        userRow.accessMap[areaId]?.value = if (backendValue == "NONE") null else AccessLevel.valueOf(backendValue)
                    } catch (e: Exception) {
                        // В случае ошибки сбрасываем таблицу к исходному состоянию
                        usersTable.refresh()
                        showAlert("Ошибка", "Не удалось сохранить права: ${e.message}")
                    }
                }
            }

            dynamicColumns.add(areaCol)
        }
        usersTable.columns.addAll(dynamicColumns)
    }
    

    private fun fillForm(user: UserDto?) {
        if (user == null) {
            clearForm()
            return
        }

        loginField.text = user.login
        fullNameField.text = user.fullName
        passwordField.clear()
        positionField.text = user.position ?: ""
        roleCombo.value = user.role
        saveButton.text = "Сохранить изменения"
        errorLabel.text = ""
    }

    private fun clearForm() {
        selectedUser = null
        loginField.clear()
        fullNameField.clear()
        passwordField.clear()
        positionField.clear()
        roleCombo.value = "ROLE_USER"
        saveButton.text = "Добавить пользователя"
        errorLabel.text = ""
        updateButtons()
    }

    private fun updateButtons() {
        saveButton.isDisable = loginField.text.trim().isBlank() || fullNameField.text.trim().isBlank() || roleCombo.value.isNullOrBlank()
    }

    private fun authHeader(): String =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private fun roleToRu(role: String): String = when (role) {
        "ROLE_ADMIN" -> "Администратор"
        "ROLE_USER" -> "Пользователь"
        else -> role
    }

    private fun showAlert(title: String, message: String) {
        errorLabel.text = message
    }

    private fun autoSizeColumnsAndWindow() {
        usersTable.columns.forEach { col ->
            var maxWidth = javafx.scene.text.Text(col.text).layoutBounds.width + 30.0
            for (i in 0 until minOf(usersTable.items.size, 50)) {
                val cellData = col.getCellData(i)?.toString() ?: ""
                val width = javafx.scene.text.Text(cellData).layoutBounds.width + 20.0
                if (width > maxWidth) maxWidth = width
            }
            col.prefWidth = maxWidth
        }

        val totalWidth = usersTable.columns.filter { it.isVisible }.sumOf { it.prefWidth } + 40.0
        val stage = usersTable.scene?.window as? Stage
        if (stage != null) {
            stage.minWidth = totalWidth
            if (stage.width < totalWidth) {
                stage.width = totalWidth
            }
        }
    }
}