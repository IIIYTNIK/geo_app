package org.example.geoapp

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import com.example.geoapp.api.GeoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.time.LocalDate
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class MainApp : Application() {


    companion object {
        val api: GeoApi by lazy {
            val gson = GsonBuilder()
                .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
                    com.google.gson.JsonPrimitive(src.toString())
                })
                .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
                    LocalDate.parse(json.asString)
                })
                .create()
            
            Retrofit.Builder()
                .baseUrl("http://localhost:8081/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(GeoApi::class.java)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }

        lateinit var instance: MainApp
    }

    override fun start(primaryStage: Stage) {
        instance = this
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