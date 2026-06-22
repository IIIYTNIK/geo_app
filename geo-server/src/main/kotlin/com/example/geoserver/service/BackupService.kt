package com.example.geoserver.backup

import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream
import javax.sql.DataSource
import kotlin.concurrent.thread

@Service
class BackupService(
    private val backupProperties: BackupProperties,
    private val dataSource: DataSource,
    private val environment: Environment // Добавили Environment для получения пароля
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Выполняет резервное копирование базы данных.
     * @return путь к созданному файлу бэкапа или null при ошибке.
     */
    fun performBackup(): File? {
        val backupDir = File(backupProperties.directory)
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            log.error("Не удалось создать директорию для бэкапов: ${backupDir.absolutePath}")
            return null
        }

        val dbParams = extractDatabaseParams()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val backupFile = File(backupDir, "geo_backup_${timestamp}.sql.gz")

        // Формируем команду pg_dump
        val command = buildList {
            add(backupProperties.pgDumpPath)
            add("-h")
            add(dbParams.host)
            add("-p")
            add(dbParams.port.toString())
            add("-U")
            add(dbParams.username)
            add("-d")
            add(dbParams.database)
            add("-F")
            add("p")   
            add("--inserts")          
            add("--no-owner")
            add("--no-privileges")
        }

        log.info("Запуск pg_dump: ${command.joinToString(" ")}")
        val processBuilder = ProcessBuilder(command)

        // Получаем пароль из application.properties (или .env) и передаем в среду выполнения pg_dump
        val password = environment.getProperty("spring.datasource.password") ?: ""
        processBuilder.environment()["PGPASSWORD"] = password

        try {
            val process = processBuilder.start()

            // Читаем поток ошибок (stderr) в отдельном потоке, чтобы процесс не завис (deadlock)
            val errorReader = thread {
                val errorOutput = process.errorStream.bufferedReader().readText()
                if (errorOutput.isNotBlank()) {
                    log.warn("Вывод pg_dump: $errorOutput")
                }
            }

            // Основной поток записывает чистые данные (stdout) в GZIP
            GZIPOutputStream(FileOutputStream(backupFile)).use { gzipOut ->
                process.inputStream.use { input ->
                    input.copyTo(gzipOut)
                }
            }

            val exitCode = process.waitFor()
            errorReader.join() // Ждем, пока дочитаются все логи ошибок

            if (exitCode == 0) {
                log.info("Резервное копирование успешно завершено. Файл: ${backupFile.absolutePath}")
                return backupFile
            } else {
                log.error("pg_dump завершился с кодом $exitCode.")
                backupFile.delete() // Обязательно удаляем битый архив размером 0 или 1 КБ
                return null
            }
        } catch (e: Exception) {
            log.error("Ошибка при выполнении pg_dump (возможно путь к pg_dump указан неверно)", e)
            backupFile.delete()
            return null
        }
    }

    /**
     * Проверяет, нужен ли бэкап (нет сегодняшнего файла).
     */
    fun isBackupNeeded(): Boolean {
        val backupDir = File(backupProperties.directory)
        if (!backupDir.exists()) return true
        val today = LocalDate.now().toString() // yyyy-MM-dd
        val todayFiles = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("geo_backup_$today")
        }
        return todayFiles.isNullOrEmpty()
    }

    /**
     * Извлекает параметры подключения из DataSource (JDBC URL).
     */
    private fun extractDatabaseParams(): DbParams {
        dataSource.connection.use { conn ->
            val url = conn.metaData.url  // jdbc:postgresql://host:port/db
            val uri = URI(url.substring(5)) // отрезаем "jdbc:"
            val host = uri.host
            val port = if (uri.port == -1) 5432 else uri.port
            val database = uri.path?.trimStart('/') ?: ""
            val username = conn.metaData.userName
            return DbParams(host, port, database, username)
        }
    }

    private data class DbParams(val host: String, val port: Int, val database: String, val username: String)
}