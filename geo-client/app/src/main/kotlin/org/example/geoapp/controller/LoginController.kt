package org.example.geoapp.controller

import org.example.geoapp.api.UserDto
import org.example.geoapp.api.GeoApi
import org.example.geoapp.api.LoginRequest
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.await

class LoginController {

    @FXML private lateinit var usernameField: TextField
    @FXML private lateinit var passwordField: PasswordField
    @FXML private lateinit var loginButton: Button
    @FXML private lateinit var errorLabel: Label
    @FXML private lateinit var progressIndicator: ProgressIndicator
    @FXML private lateinit var root: VBox
    @FXML private lateinit var settingsButton: Button

    private val api: GeoApi get() = MainApp.api

    @FXML
    fun initialize() {
        progressIndicator.isVisible = false
        errorLabel.text = ""
    }

    @FXML
    fun onLogin() {
        val username = usernameField.text.trim()
        val password = passwordField.text.trim()

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.text = "Заполните все поля"
            return
        }

        loginButton.isDisable = true
        progressIndicator.isVisible = true
        errorLabel.text = ""

        runOnFx {
            try {
                val response = api.login(LoginRequest(username, password)).await()
                openMainWindow(response.token, response.role, response.user)
            } catch (e: Exception) {
                errorLabel.text = when {
                    e.message?.contains("HTTP 401") == true -> "Неверный логин или пароль"
                    e.message?.contains("Failed to connect") == true -> "Сервер недоступен.\nПроверьте настройки подключения (⚙️)"
                    else -> "Ошибка сети: ${e.message}"
                }
                loginButton.isDisable = false
                progressIndicator.isVisible = false
            }
        }
    }

    @FXML
    fun openSettings() {
        val loader = FXMLLoader(javaClass.getResource("/settings.fxml"))
        val root = loader.load< javafx.scene.Parent>()
        val stage = Stage()
        stage.title = "Настройки подключения"
        stage.scene = Scene(root)
        stage.isResizable = false
        stage.initOwner(this.root.scene.window as Stage)
        stage.showAndWait()  // ждём закрытия окна настроек
    }

    private fun openMainWindow(token: String, role: String, currentUser: UserDto) {
        val loader = FXMLLoader(javaClass.getResource("/main.fxml"))
        val root = loader.load<javafx.scene.Parent>()
        val controller = loader.getController<MainController>()
        controller.initData(token, role, currentUser)

        val stage = (this.root.scene.window as Stage)
        stage.scene = Scene(root)
        stage.title = "GeoApp - Главная"
    }
}