package org.example.geoapp.api

import org.example.geoapp.api.report.*
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.*

interface GeoApi {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // --- Пользователи ---
    @GET("api/users") fun getUsers(@Header("Authorization") token: String): Call<List<UserDto>>
    @GET("api/users/me/access") fun getCurrentUserAccess(@Header("Authorization") token: String): Call<List<UserAreaAccessDto>>
    @POST("api/users") fun createUser(@Header("Authorization") token: String, @Body user: UserCreateDto): Call<UserDto>
    @PUT("api/users/{id}") fun updateUser(@Header("Authorization") token: String, @Path("id") id: Long, @Body user: UserUpdateDto): Call<UserDto>
    @DELETE("api/users/{id}") fun deleteUser(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @GET("api/users/access/all") fun getAllUsersAccess(@Header("Authorization") token: String): Call<List<UserAreaAccessDto>>
    @PUT("api/users/{userId}/access/{areaId}") fun updateUserAccess(@Header("Authorization") token: String, @Path("userId") userId: Long, @Path("areaId") areaId: Long, @Query("level") level: String): Call<Void>

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
    @GET("api/workings/recycle-bin") fun getRecycleBin(@Header("Authorization") token: String): Call<List<Working>>
    @POST("api/workings/{id}/restore") fun restoreWorking(@Header("Authorization") token: String, @Path("id") id: Long): Call<Working>
    @GET("api/audit/workings") fun getWorkingAuditHistory(@Header("Authorization") token: String): Call<List<WorkingAuditEntry>>
    @POST("api/audit/workings/{workingId}/restore-revision/{revision}") fun restoreRevision(@Header("Authorization") token: String, @Path("workingId") workingId: Long, @Path("revision") revision: Int): Call<Working>

    // Метод для массовой загрузки выработок
    @POST("api/workings/batch") fun createBatch(@Header("Authorization") token: String, @Body workings: List<Working>): Call<List<Working>>

    // --- Шаблны отчётов ---
    @GET("api/report-templates") fun getReportTemplates(@Header("Authorization") token: String): Call<List<ReportTemplateSummaryDto>>
    @GET("api/report-templates/{id}") fun getTemplate(@Header("Authorization") token: String, @Path("id") id: Long): Call<ReportTemplateDto>
    @DELETE("api/report-templates/{id}") fun deleteTemplate(@Header("Authorization") token: String, @Path("id") id: Long): Call<Void>
    @GET("api/report-templates/{id}/metadata") fun getReportTemplateMetadata(@Header("Authorization") token: String, @Path("id") id: Long): Call<ReportTemplateMetadataDto>
    @POST("api/reports/generate") fun generateReport(@Header("Authorization") token: String, @Body request: ReportGenerateRequest): Call<ResponseBody>
    @Multipart @POST("api/report-templates/upload") fun uploadTemplate(@Header("Authorization") token: String, @Part("name") name: RequestBody, @Part("description") description: RequestBody?,  @Part file: MultipartBody.Part, @Part("overwrite") overwrite: RequestBody? ): Call<ReportTemplateSummaryDto>

}

data class LoginRequest(val login: String, val password: String)
data class LoginResponse(val token: String, val role: String, val user: UserDto)

// Модели справочников
data class RefArea(val id: Long = 0, val name: String, val comment: String? = null)
data class RefContractor(val id: Long = 0, val name: String, val comment: String? = null)
data class RefDrillingRig(val id: Long = 0, val name: String, val alias: String? = null, val comment: String? = null)
data class RefWorkType(val id: Long = 0, val name: String, val comment: String? = null)
data class RefGeologist(val id: Long = 0, val name: String, val alias: String? = null, var contractor: RefContractor? = null, val position: String? = null, val comment: String? = null)

// Модели для работы с пользователями
data class UserDto(val id: Long, val login: String, val fullName: String, val role: String, val position: String?)
data class UserCreateDto(val login: String, val fullName: String, val password: String, val role: String, val position: String? = null)
data class UserUpdateDto(val login: String, val fullName: String, val role: String, val password: String?, val position: String? = null)
data class UserAreaAccessDto(val areaId: Long, val accessLevel: AccessLevel, val userId: Long? = null)
enum class AccessLevel { READ, WRITE }

// DTO для изменённых полей
data class ChangedFieldDto(val field: String, val oldValue: String?, val newValue: String?)
data class WorkingAuditEntry(val workingId: Long, val revisionNumber: Int, val revisionType: String, val revisionTimestamp: String, val username: String?, val objectName: String, val details: WorkingAuditDetailsDto, val changesText: String)
data class WorkingAuditDetailsDto(val id: Long, val number: String?, val areaName: String?, val contractorName: String?, val geologistName: String?, val workTypeName: String?) // Краткая информация о выработке для аудита


// Модель выработки (Working) - основная сущность, с которой будем работать в приложении
data class Working(
    val id: Long = 0,
    val area: RefArea? = null,
    var workType: RefWorkType? = null, 
    var plannedContractor: RefContractor? = null,
    var number: String, 
    var structure: String? = null,            
    
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
    var actNumber: String? = null,
    var thermalTube: Boolean = false,
    var hasVideo: Boolean = false,
    var hasDrilling: Boolean = false,
    var hasJournal: Boolean = false,
    var hasCore: Boolean = false,
    var hasStake: Boolean = false,
    
    var emergency: Boolean = false, // Аварийная скважина
    var mediaPath: String? = null, // Путь к медиафайлам (видео, фото) для отображения в карточке скважины
    var samplesThawed: Int? = null,
    var samplesFrozen: Int? = null,
    var samplesRocky: Int? = null,

    var isProject: Boolean = false,

    var cat1_4: Double? = null,
    var cat5_8: Double? = null,
    var cat9_12: Double? = null,
)

