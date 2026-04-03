package com.example.geoapp.api

import retrofit2.Call
import retrofit2.http.*

interface GeoApi {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // --- Чтение (GET) ---
    @GET("api/references/contractors") fun getContractors(): Call<List<RefContractor>>
    @GET("api/references/areas") fun getAreas(): Call<List<RefArea>>
    @GET("api/references/geologists") fun getGeologists(): Call<List<RefGeologist>>
    @GET("api/references/drilling-rigs") fun getDrillingRigs(): Call<List<RefDrillingRig>>
    @GET("api/references/work-types") fun getWorkTypes(): Call<List<RefWorkType>>
    @GET("api/references/geologists/by-contractor/{id}") fun getGeologistsByContractor(@Header("Authorization") token: String, @Path("id") id: Long): Call<List<RefGeologist>>

    // --- Создание (POST) ---
    @POST("api/references/areas") fun createArea(@Header("Authorization") token: String, @Body item: RefArea): Call<RefArea>
    @POST("api/references/work-types") fun createWorkType(@Header("Authorization") token: String, @Body item: RefWorkType): Call<RefWorkType>
    @POST("api/references/drilling-rigs") fun createDrillingRig(@Header("Authorization") token: String, @Body item: RefDrillingRig): Call<RefDrillingRig>
    @POST("api/references/contractors") fun createContractor(@Header("Authorization") token: String, @Body item: RefContractor): Call<RefContractor>
    @POST("api/references/geologists") fun createGeologist(@Header("Authorization") token: String, @Body item: RefGeologist): Call<RefGeologist>

    // --- Обновление (PUT) ---
    @PUT("api/references/areas/{id}") fun updateArea(@Header("Authorization") token: String, @Path("id") id: Long, @Body item: RefArea): Call<RefArea>
    @PUT("api/references/work-types/{id}") fun updateWorkType(@Header("Authorization") token: String, @Path("id") id: Long, @Body item: RefWorkType): Call<RefWorkType>
    @PUT("api/references/drilling-rigs/{id}") fun updateDrillingRig(@Header("Authorization") token: String, @Path("id") id: Long, @Body item: RefDrillingRig): Call<RefDrillingRig>
    @PUT("api/references/contractors/{id}") fun updateContractor(@Header("Authorization") token: String, @Path("id") id: Long, @Body item: RefContractor): Call<RefContractor>
    @PUT("api/references/geologists/{id}") fun updateGeologist(@Header("Authorization") token: String, @Path("id") id: Long, @Body item: RefGeologist): Call<RefGeologist>

    // --- Удаление (DELETE) ---
    @DELETE("api/references/areas/{id}") fun deleteArea(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @DELETE("api/references/work-types/{id}") fun deleteWorkType(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @DELETE("api/references/drilling-rigs/{id}") fun deleteDrillingRig(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @DELETE("api/references/contractors/{id}") fun deleteContractor(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @DELETE("api/references/geologists/{id}") fun deleteGeologist(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>

    // --- Выработки ---
    @GET("api/workings") fun getWorkings(@Header("Authorization") token: String): Call<List<Working>>
    @GET("api/workings/{id}") fun getWorking(@Header("Authorization") token: String, @Path("id") id: Long): Call<Working>
    @POST("api/workings") fun createWorking(@Header("Authorization") token: String, @Body working: Working): Call<Working>
    @PUT("api/workings/{id}") fun updateWorking(@Header("Authorization") token: String, @Path("id") id: Long, @Body working: Working): Call<Working>
    @DELETE("api/workings/{id}") fun deleteWorking(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>

    // Метод для массовой загрузки выработок (например, из Excel)
    @POST("api/workings/batch") fun createWorkingsBatch(@Header("Authorization") token: String, @Body workings: List<Working>): Call<List<Working>>
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val role: String)

// Модели справочников
data class RefArea(val id: Long = 0, val name: String, val comment: String? = null)
data class RefContractor(val id: Long = 0, val name: String, val comment: String? = null)
data class RefDrillingRig(val id: Long = 0, val name: String, val alias: String? = null, val comment: String? = null)
data class RefWorkType(val id: Long = 0, val name: String, val comment: String? = null)
data class RefGeologist(val id: Long = 0, val name: String, val alias: String? = null, var contractor: RefContractor? = null, val comment: String? = null)

// Модель выработки (Working) - основная сущность, с которой будем работать в приложении
data class Working(
    val id: Long = 0,
    val area: RefArea? = null,
    var workType: RefWorkType? = null, 
    var number: String,             
    
    var orderNum: Int? = null, // Для отображения порядкового номера в таблице
    
    val plannedX: Double? = null,
    val plannedY: Double? = null,
    val plannedDepth: Double? = null,

    val actualX: Double? = null,
    val actualY: Double? = null,
    val actualZ: Double? = null,
    val actualDepth: Double? = null,

    val deltaS: Double? = null,

    val startDate: String? = null,
    val endDate: String? = null,
    
    val geologist: RefGeologist? = null,
    val contractor: RefContractor? = null,
    val drillingRig: RefDrillingRig? = null,
    
    val additionalInfo: String? = null,
    
    val coreRecovery: Double? = null,
    val casing: Double? = null,
    
    val mmg1Top: Double? = null,
    val mmg1Bottom: Double? = null,
    val mmg2Top: Double? = null,
    val mmg2Bottom: Double? = null,
    
    val gwAppearLog: Double? = null,
    val gwStableLog: Double? = null,
    val gwStableAbs: Double? = null,

    // Чекбоксы (Boolean)
    var act: Boolean = false,
    val actNumber: String? = null,
    var thermalTube: Boolean = false,
    var hasVideo: Boolean = false,
    var hasDrilling: Boolean = false,
    var hasJournal: Boolean = false,
    var hasCore: Boolean = false,
    var hasStake: Boolean = false,

    val samplesThawed: Int? = null,
    val samplesFrozen: Int? = null,
    val samplesRocky: Int? = null,

    val isProject: Boolean = false
)