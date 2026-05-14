package org.example.geoapp.controller

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.example.geoapp.MainApp

class SettingsController {

    @FXML private lateinit var ipField: TextField
    @FXML private lateinit var portField: TextField
    @FXML private lateinit var statusLabel: Label

    @FXML
    fun initialize() {
        // Загружаем текущий URL из настроек и разбираем его
        val currentUrl = MainApp.AppConfig.getServerUrl()
        try {
            val hostPart = currentUrl.substringAfter("://").trimEnd('/')
            val ip = hostPart.substringBefore(":")
            val port = hostPart.substringAfter(":", "8081")
            ipField.text = ip
            portField.text = port
        } catch (e: Exception) {
            ipField.text = "192.168.0.1"
            portField.text = "8081"
        }
        statusLabel.text = ""
    }

    @FXML
    fun saveSettings() {
        val ip = ipField.text.trim()
        val port = portField.text.trim()

        if (ip.isEmpty() || port.isEmpty()) {
            statusLabel.text = "IP и порт не могут быть пустыми"
            statusLabel.style = "-fx-text-fill: red;"
            return
        }
        if (!port.matches(Regex("\\d+"))) {
            statusLabel.text = "Порт должен быть числом"
            statusLabel.style = "-fx-text-fill: red;"
            return
        }

        val newUrl = "http://$ip:$port/"
        MainApp.AppConfig.setServerUrl(newUrl)
        MainApp.recreateApi()  // обновляем API во всём приложении

        statusLabel.text = "Настройки сохранены"
        statusLabel.style = "-fx-text-fill: green;"

        // Можно автоматически закрыть окно через 1 секунду
        Thread {
            Thread.sleep(1000)
            javafx.application.Platform.runLater { closeWindow() }
        }.start()
    }

    @FXML
    fun closeWindow() {
        (ipField.scene.window as Stage).close()
    }
}