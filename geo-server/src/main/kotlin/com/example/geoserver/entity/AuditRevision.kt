package com.example.geoserver.entity

import com.example.geoserver.security.SecurityUtils
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener

// Слушатель ревизий
class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any?) {
        if (revisionEntity is AuditRevisionEntity) {
            revisionEntity.username = SecurityUtils.currentUser()?.username
        }
    }
}

// Сущность ревизии
@Entity
@RevisionEntity(AuditRevisionListener::class)
@Table(name = "revinfo")
class AuditRevisionEntity : DefaultRevisionEntity() {
    @Column(name = "username")
    var username: String? = null
}

