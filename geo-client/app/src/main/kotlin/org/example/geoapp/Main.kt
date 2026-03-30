package org.example.geoapp

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import com.example.geoapp.api.GeoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainApp : Application() {


    companion object {
        val api: GeoApi by lazy {
            Retrofit.Builder()
                .baseUrl("http://localhost:8081/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeoApi::class.java)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }
    }

    override fun start(primaryStage: Stage) {
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
}