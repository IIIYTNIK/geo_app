package com.example.geoserver.service

import com.example.geoserver.entity.Working
import com.example.geoserver.repository.WorkingRepository
import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.springframework.stereotype.Service
import java.time.Instant
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.math.BigDecimal

// Обновленный DTO. Теперь тут есть поле changesText для вывода в интерфейс
data class WorkingAuditEntry(
    val workingId: Long,
    val revisionNumber: Int,
    val revisionType: String,
    val revisionTimestamp: String,
    val username: String?,
    val objectName: String,
    val details: WorkingAuditDetailsDto,
    val changesText: String 
)

data class WorkingAuditDetailsDto(
    val id: Long,
    val number: String?,
    val areaName: String?,
    val contractorName: String?,
    val geologistName: String?,
    val workTypeName: String?
)

@Service
class AuditService(
    private val entityManager: EntityManager, 
    private val workingRepository: WorkingRepository
) {
    @Transactional(readOnly = true)
    fun getWorkingAuditEntries(): List<WorkingAuditEntry> {
        val auditReader = AuditReaderFactory.get(entityManager)
        val query = auditReader.createQuery().forRevisionsOfEntity(Working::class.java, false, true)
        val results = query.resultList as List<Array<Any>>

        // 1. Группируем историю по ID скважины, чтобы история не перемешивалась
        val historyByWorkingId = results.groupBy { (it[0] as Working).id }
        val finalEntries = mutableListOf<WorkingAuditEntry>()

        // 2. Идем по каждой скважине отдельно
        for ((workingId, history) in historyByWorkingId) {
            // Сортируем ревизии от старых к новым
            val sortedHistory = history.sortedBy { (it[1] as org.hibernate.envers.DefaultRevisionEntity).id }
            
            var previousWorking: Working? = null

            for (array in sortedHistory) {
                val working = array[0] as Working
                val revisionEntity = array[1] as org.hibernate.envers.DefaultRevisionEntity
                val type = array[2] as RevisionType
                
                // Получение имени пользователя (безопасный каст)
                val username = try {
                    val customRev = revisionEntity as? com.example.geoserver.entity.AuditRevisionEntity // Укажи свой путь к классу
                    customRev?.username
                } catch (e: Exception) { null }

                // Генерируем красивый текст изменений
                val diffText = when (type) {
                    RevisionType.ADD -> "Создана новая выработка"
                    RevisionType.DEL -> "Выработка удалена"
                    RevisionType.MOD -> generateDiffText(previousWorking, working)
                    else -> "Неизвестная операция"
                }

                finalEntries.add(
                    WorkingAuditEntry(
                        workingId = working.id,
                        revisionType = when (type) {
                            RevisionType.ADD -> "Создано"
                            RevisionType.MOD -> "Изменено"
                            RevisionType.DEL -> "Удалено"
                            else -> type.name
                        },
                        revisionNumber = revisionEntity.id,
                        revisionTimestamp = Instant.ofEpochMilli(revisionEntity.timestamp).toString(),
                        username = username ?: "Система",
                        objectName = "Скважина ${working.number ?: "б/н"}",
                        changesText = diffText,
                        details = WorkingAuditDetailsDto(
                            id = working.id,
                            number = working.number,
                            areaName = working.area?.name,
                            contractorName = working.contractor?.name,
                            geologistName = working.geologist?.name,
                            workTypeName = working.workType?.name
                        )
                    )
                )
                // Запоминаем текущую скважину, чтобы на следующем шаге цикла сравнить с ней
                previousWorking = working
            }
        }
        
        // Возвращаем список, отсортированный по времени (самые новые события сверху)
        return finalEntries.sortedByDescending { it.revisionTimestamp }
    }

    @Transactional
    fun restoreRevision(workingId: Long, revision: Int): Working {
        val auditReader = AuditReaderFactory.get(entityManager)
        val oldVersion = auditReader.find(Working::class.java, workingId, revision)
            ?: throw RuntimeException("Ревизия не найдена")

        // Проверка дубликата номера
        val existing = workingRepository.findByNumberAndAreaId(oldVersion.number, oldVersion.area?.id)
        if (existing != null && existing.id != workingId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_NUMBER")
        }

        return workingRepository.save(oldVersion.copy(id = workingId, isDeleted = false))
    }

    private fun formatValue(value: Any?): String {
        if (value == null) return "пусто"

        // Если это справочник, пытаемся достать его имя
        if (value.javaClass.simpleName.startsWith("Ref")) {
            return try {
                value.javaClass.getMethod("getName").invoke(value).toString()
            } catch (e: Exception) {
                value.toString()
            }
        }

        // Если это число с плавающей точкой, убираем экспоненту (E)
        if (value is Double || value is Float) {
            // toPlainString() заставит число 1.45E7 выглядеть как 14500000.0
            return BigDecimal(value.toString()).toPlainString()
        }

        return value.toString()
    }

    private fun generateDiffText(oldState: Working?, newState: Working): String {
        if (oldState == null) return "Создана новая выработка"

        val changes = mutableListOf<String>()
        
        val fieldNames = mapOf(
            "actualDepth" to "Факт H", "plannedDepth" to "План H", "emergency" to "Аварийная",
            "workType" to "Тип выработки", "contractor" to "Подрядчик", "isDeleted" to "Статус удаления",
            "area" to "Участок", "geologist" to "Геолог", "drillingRig" to "Буровая",
            "structure" to "Сооружение", "plannedContractor" to "Плановый подрядчик",
            "plannedX" to "План X", "plannedY" to "План Y", "actualX" to "Факт X",
            "actualY" to "Факт Y", "actualZ" to "Факт Z", "coreRecovery" to "Выход керна, %",
            "casing" to "Обсад", "startDate" to "Дата начала", "endDate" to "Дата окончания",
            "mmg1Top" to "ММГ1 кровля", "mmg1Bottom" to "ММГ1 подошва", "mmg2Top" to "ММГ2 кровля",
            "mmg2Bottom" to "ММГ2 подошва", "gwAppearLog" to "ПУГВ", "gwStableLog" to "УУГВ",
            "act" to "Акт", "actNumber" to "№ акта", "thermalTube" to "Термотрубка",
            "hasVideo" to "Видео", "hasDrilling" to "Буровая (материал)", "hasJournal" to "Журнал",
            "hasCore" to "Керн", "hasStake" to "Штага", "samplesThawed" to "Т, шт",
            "samplesFrozen" to "М, шт", "samplesRocky" to "С, шт", "isProject" to "Проект",
            "cat1_4" to "Кат 1-4, п.м.", "cat5_8" to "Кат 5-8, п.м.", "cat9_12" to "Кат 9-12, п.м."
        )

        val excludedFields = setOf("id", "auditUpdatedAt", "auditUpdateAt", "version", "createdAt")
        
        for (prop in Working::class.java.declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(prop.modifiers)) continue
            prop.isAccessible = true
            
            val oldValue = prop.get(oldState)
            val newValue = prop.get(newState)

            if (oldValue != newValue && !excludedFields.contains(prop.name)) { // Не показываем изменение ID и даты изменения, так как это несущественно для пользователя
                val readableName = fieldNames[prop.name] ?: prop.name
                
                // Вытаскиваем имена из справочников, а если null - пишем "пусто"
                val oldStr = if (oldValue != null && oldValue.javaClass.simpleName.startsWith("Ref")) {
                    oldValue.javaClass.getMethod("getName").invoke(oldValue)
                } else oldValue?.toString() ?: "пусто"
                
                val newStr = if (newValue != null && newValue.javaClass.simpleName.startsWith("Ref")) {
                    newValue.javaClass.getMethod("getName").invoke(newValue)
                } else newValue?.toString() ?: "пусто"

                changes.add("* $readableName: $oldStr -> $newStr")
            }
        }

        return if (changes.isEmpty()) "Без изменений" else changes.joinToString("\n")
    }
}