package com.example.geoserver.backup

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class BackupScheduler(
    private val backupService: BackupService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Выполняем бэкап каждый день в 2 часа ночи
    @Scheduled(cron = "0 0 2 * * *")
    fun scheduledBackup() {
        log.info("Запуск планового резервного копирования")
        backupService.performBackup()
    }

    // Выполняем проверку и бэкап при старте приложения, если сегодняшнего бэкапа нет
    @EventListener(ApplicationReadyEvent::class)
    fun backupOnStartup() {
        if (backupService.isBackupNeeded()) {
            log.info("Сегодняшний бэкап не найден, запускаем при старте приложения")
            backupService.performBackup()
        } else {
            log.info("Сегодняшний бэкап уже существует, пропускаем автоматический запуск")
        }
    }
}