package com.example.geoserver.controller

import com.example.geoserver.backup.BackupService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class BackupController(
    private val backupService: BackupService
) {

    @PostMapping("/backup")
    @Secured("ROLE_ADMIN")
    fun manualBackup(): ResponseEntity<Map<String, Any>> {
        val backupFile = backupService.performBackup()
        return if (backupFile != null) {
            ResponseEntity.ok(mapOf(
                "message" to "Бэкап успешно создан",
                "file" to backupFile.absolutePath
            ))
        } else {
            ResponseEntity.status(500).body(mapOf("error" to "Не удалось создать бэкап"))
        }
    }
}