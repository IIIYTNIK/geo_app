package com.example.geoserver.controller

import com.example.geoserver.service.AuditService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val auditService: AuditService
) {

    @GetMapping("/workings")
    fun getWorkingAudit(): List<Any> {
        return auditService.getWorkingAuditEntries()
    }
}
