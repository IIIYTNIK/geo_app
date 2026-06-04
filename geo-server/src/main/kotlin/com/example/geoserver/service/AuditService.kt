package com.example.geoserver.service

import com.example.geoserver.entity.Working
import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuditService(
    private val entityManager: EntityManager
) {

    fun getWorkingAuditEntries(): List<WorkingAuditEntry> {
        val auditReader = AuditReaderFactory.get(entityManager)
        val query = auditReader.createQuery().forRevisionsOfEntity(Working::class.java, false, true)
        val results = query.resultList as List<Array<Any>>

        return results.map { array ->
            val working = array[0] as Working
            val revisionEntity = array[1]
            val type = array[2] as RevisionType

            val username = revisionEntity?.let {
                when (it) {
                    is com.example.geoserver.entity.AuditRevisionEntity -> it.username
                    is org.hibernate.envers.DefaultRevisionEntity -> null
                    else -> null
                }
            }

            WorkingAuditEntry(
                workingId = working.id,
                revisionType = when (type) {
                    RevisionType.ADD -> "Создано"
                    RevisionType.MOD -> "Изменено"
                    RevisionType.DEL -> "Удалено"
                    else -> type.name
                },
                revisionTimestamp = Instant.ofEpochMilli((revisionEntity as org.hibernate.envers.DefaultRevisionEntity).timestamp).toString(),
                username = username,
                objectName = "Working",
                details = working
            )
        }
    }
}

data class WorkingAuditEntry(
    val workingId: Long,
    val revisionType: String,
    val revisionTimestamp: String,
    val username: String?,
    val objectName: String,
    val details: Working
)
