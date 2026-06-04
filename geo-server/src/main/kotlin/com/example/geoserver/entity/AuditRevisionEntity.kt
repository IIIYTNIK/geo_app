package com.example.geoserver.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity

@Entity
@RevisionEntity(AuditRevisionListener::class)
@Table(name = "revinfo")
class AuditRevisionEntity : DefaultRevisionEntity() {
    @Column(name = "username")
    var username: String? = null
}
