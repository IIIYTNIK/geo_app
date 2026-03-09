package com.example.geoapp.api

import retrofit2.Call
import retrofit2.http.*

interface GeoApi {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Справочники (открытые)
    @GET("api/references/contractors")
    fun getContractors(): Call<List<RefContractor>>

    @GET("api/references/areas")
    fun getAreas(): Call<List<RefArea>>

    @GET("api/references/geologists")
    fun getGeologists(): Call<List<RefGeologist>>

    @GET("api/references/drilling-rigs")
    fun getDrillingRigs(): Call<List<RefDrillingRig>>

    @GET("api/references/work-types")
    fun getWorkTypes(): Call<List<RefWorkType>>

    // CRUD выработок (защищённые)
    @GET("api/workings")
    fun getWorkings(@Header("Authorization") token: String): Call<List<Working>>

    @GET("api/workings/{id}")
    fun getWorking(@Header("Authorization") token: String, @Path("id") id: Long): Call<Working>

    @POST("api/workings")
    fun createWorking(@Header("Authorization") token: String, @Body working: Working): Call<Working>

    @PUT("api/workings/{id}")
    fun updateWorking(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body working: Working
    ): Call<Working>

    @DELETE("api/workings/{id}")
    fun deleteWorking(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>

    @GET("api/references/geologists/by-contractor/{contractorId}")
    fun getGeologistsByContractor(
        @Header("Authorization") token: String,
        @Path("contractorId") contractorId: Long
    ): Call<List<RefGeologist>>
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val role: String)

// Модели справочников (копируем из сервера, но упрощённо)
data class RefArea(val id: Long, val name: String)
data class RefContractor(val id: Long, val name: String)
data class RefDrillingRig(val id: Long, val name: String)
data class RefGeologist(val id: Long, val name: String)
data class RefWorkType(val id: Long, val name: String)

// Модель выработки (Working) - основная сущность, с которой будем работать в приложении
data class Working(
    val id: Long = 0,
    val area: RefArea? = null,
    val workType: RefWorkType? = null,
    val number: String = "",
    val plannedX: Double? = null,
    val plannedY: Double? = null,
    val plannedZ: Double? = null,
    val actualX: Double? = null,
    val actualY: Double? = null,
    val actualZ: Double? = null,
    val depth: Double? = null,
    val startDate: String? = null,  
    val endDate: String? = null,
    val geologist: RefGeologist? = null,
    val contractor: RefContractor? = null,
    val drillingRig: RefDrillingRig? = null,
    val additionalInfo: String? = null,
    val coreRecovery: Double? = null,          
    val casing: String? = null,       
    val closureStage: String? = null,
    val mmg1Top: Double? = null,
    val mmg1Bottom: Double? = null,
    val mmg2Top: Double? = null,
    val mmg2Bottom: Double? = null,
    val gwAppearLog: Double? = null,
    val gwStableLog: Double? = null,
    val gwStableAbs: Double? = null,
    val gwStableRel: Double? = null,
    val gwStableAbsFinal: Double? = null,
    val contractorExtraIndex: String? = null,
    val act: String? = null,
    val actNumber: String? = null,
    val thermalTube: String? = null
)