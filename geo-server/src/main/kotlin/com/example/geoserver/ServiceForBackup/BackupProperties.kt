package com.example.geoserver.backup

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "backup")
class BackupProperties {
    var pgDumpPath: String = "pg_dump"   // путь к исполняемому файлу pg_dump
    var directory: String = "./backups"  // папка для хранения бэкапов
}