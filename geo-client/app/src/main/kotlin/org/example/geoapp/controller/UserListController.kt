package org.example.geoapp.controller

import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.UserCreateDto
import org.example.geoapp.api.UserDto
import org.example.geoapp.api.UserUpdateDto
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.await
import org.example.geoapp.util.awaitUnit
import org.example.geoapp.util.runOnFx

class UserListController {

    @FXML private lateinit var usersTable: TableView<UserDto>
    @FXML private lateinit var colId: TableColumn<UserDto, Long>
    @FXML private lateinit var colUsername: TableColumn<UserDto, String>
    @FXML private lateinit var colRole: TableColumn<UserDto, String>
    @FXML private lateinit var colPosition: TableColumn<UserDto, String>

    @FXML private lateinit var usernameField: TextField
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

    fun initData(token: String) {
        this.token = token
        setupTable()
        setupForm()
        loadUsers()
        clearForm()
    }

    private fun setupTable() {
        colId.setCellValueFactory { SimpleObjectProperty(it.value.id) }
        colUsername.setCellValueFactory { SimpleStringProperty(it.value.username) }
        colRole.setCellValueFactory { SimpleStringProperty(roleToRu(it.value.role)) }
        colPosition.setCellValueFactory { SimpleStringProperty(it.value.position ?: "") }

        usersTable.isEditable = false
        usersTable.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            selectedUser = newValue
            fillForm(newValue)
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

        usernameField.textProperty().addListener { _, _, _ -> updateButtons() }
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
        val username = usernameField.text.trim()
        val role = roleCombo.value
        val password = passwordField.text

        if (username.isBlank()) {
            showAlert("Ошибка", "Введите имя пользователя")
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
                        UserCreateDto(
                            username = username,
                            password = password,
                            role = role.removePrefix("ROLE_"),
                            position = positionField.text.trim() ?: ""
                        )
                    ).await()
                } else {
                    api.updateUser(
                        tokenStr,
                        current.id,
                        UserUpdateDto(
                            username = username,
                            role = role,
                            password = password.trim().ifBlank { null },
                            position = positionField.text.trim() ?: ""
                        )
                    ).await()
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

        val confirm = Alert(
            Alert.AlertType.CONFIRMATION,
            "Удалить пользователя '${selected.username}'?",
            ButtonType.YES,
            ButtonType.NO
        )
        confirm.dialogPane.graphic = null

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return

        runOnFx {
            try {
                api.deleteUser(authHeader(), selected.id).awaitUnit()
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
                val users = api.getUsers(authHeader()).await().sortedBy { it.id }
                usersTable.items = FXCollections.observableArrayList(users)
                autoSizeColumnsAndWindow()
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось загрузить пользователей: ${e.message}")
            }
        }
    }

    private fun fillForm(user: UserDto?) {
        if (user == null) {
            clearForm()
            return
        }

        usernameField.text = user.username
        passwordField.clear()
        positionField.text = user.position ?: ""
        roleCombo.value = user.role
        saveButton.text = "Сохранить изменения"
        errorLabel.text = ""
    }

    private fun clearForm() {
        selectedUser = null
        usernameField.clear()
        passwordField.clear()
        positionField.clear()
        roleCombo.value = "ROLE_USER"
        saveButton.text = "Добавить пользователя"
        errorLabel.text = ""
        updateButtons()
    }

    private fun updateButtons() {
        saveButton.isDisable =
            usernameField.text.trim().isBlank() ||
            roleCombo.value.isNullOrBlank()
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
        // Alert(Alert.AlertType.ERROR).apply {
        //     this.title = title
        //     headerText = null
        //     contentText = message
        //     showAndWait()
        // }
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