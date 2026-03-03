package org.example.geoapp.controller

import com.example.geoapp.api.GeoApi
import com.example.geoapp.api.LoginRequest
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.geoapp.MainApp
import org.example.geoapp.util.runOnFx
import org.example.geoapp.util.await
import javafx.scene.Scene

class LoginController {

    @FXML
    private lateinit var usernameField: TextField

    @FXML
    private lateinit var passwordField: PasswordField

    @FXML
    private lateinit var loginButton: Button

    @FXML
    private lateinit var errorLabel: Label

    @FXML
    private lateinit var progressIndicator: ProgressIndicator

    @FXML
    private lateinit var root: VBox

    private val api: GeoApi = MainApp.api

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
                // Успешный вход
                openMainWindow(response.token)
            } catch (e: Exception) {
                errorLabel.text = when {
                    e.message?.contains("HTTP 401") == true -> "Неверный логин или пароль"
                    else -> "Ошибка сети: ${e.message}"
                }
                loginButton.isDisable = false
                progressIndicator.isVisible = false
            }
        }
    }

    private fun openMainWindow(token: String) {
        val loader = FXMLLoader(javaClass.getResource("/main.fxml"))
        val root = loader.load<javafx.scene.Parent>()
        val controller = loader.getController<MainController>()
        controller.setToken(token)

        val stage = (this.root.scene.window as Stage)
        stage.scene = Scene(root)
        stage.title = "GeoApp - Главная"
    }
}