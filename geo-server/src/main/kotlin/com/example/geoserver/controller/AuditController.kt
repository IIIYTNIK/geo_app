package com.example.geoserver.controller

import com.example.geoserver.service.AuditService
import com.example.geoserver.service.WorkingAuditEntry
import com.example.geoserver.entity.Working
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val auditService: AuditService
) {
    @GetMapping("/workings")
    fun getWorkingAudit(): List<WorkingAuditEntry> = auditService.getWorkingAuditEntries()

    @PostMapping("/workings/{workingId}/restore-revision/{revision}")
    fun restoreRevision(@PathVariable workingId: Long, @PathVariable revision: Int): Working {return auditService.restoreRevision(workingId, revision)}
}