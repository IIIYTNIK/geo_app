package org.example.geoapp

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.example.geoapp.api.GeoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.prefs.Preferences

class MainApp : Application() {

    companion object {
        lateinit var api: GeoApi

        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }

        lateinit var instance: MainApp

        /** Пересоздаёт Retrofit-клиент с текущим URL из настроек */
        fun recreateApi() {
            val serverUrl = AppConfig.getServerUrl()
            api = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeoApi::class.java)
        }
    }

    override fun start(primaryStage: Stage) {
        instance = this
        // Инициализируем API перед показом окна входа
        recreateApi()
        showLoginWindow()
    }

    fun showLoginWindow() {
        val loader = FXMLLoader(javaClass.getResource("/login.fxml"))
        val root = loader.load<javafx.scene.Parent>()
        val stage = Stage()
        stage.scene = Scene(root)
        stage.title = "GeoApp - Вход"
        stage.show()
    }

    /** Объект для хранения настроек подключения в реестре (Preferences) */
    object AppConfig {
        private const val PREFS_NODE = "geoapp"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_URL = "http://192.168.0.1:8081/"

        private val prefs = Preferences.userRoot().node(PREFS_NODE)

        fun getServerUrl(): String = prefs.get(KEY_SERVER_URL, DEFAULT_URL)

        fun setServerUrl(url: String) {
            prefs.put(KEY_SERVER_URL, url)
        }
    }
}