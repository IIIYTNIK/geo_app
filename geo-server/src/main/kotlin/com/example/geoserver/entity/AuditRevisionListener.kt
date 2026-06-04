package com.example.geoserver.entity

import com.example.geoserver.security.SecurityUtils
import org.hibernate.envers.RevisionListener

class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any?) {
        if (revisionEntity is AuditRevisionEntity) {
            revisionEntity.username = SecurityUtils.currentUser()?.username
        }
    }
}
