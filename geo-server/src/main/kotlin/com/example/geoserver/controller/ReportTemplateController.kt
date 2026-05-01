package com.example.geoserver.controller

import com.example.geoserver.dto.report.ReportOptionDto
import com.example.geoserver.dto.report.ReportParameterDto
import com.example.geoserver.dto.report.ReportParameterType
import com.example.geoserver.dto.report.ReportValueType
import com.example.geoserver.dto.report.ReportTemplateDto
import com.example.geoserver.dto.report.ReportTemplateMetadataDto
import com.example.geoserver.dto.report.ReportTemplateSummaryDto
import com.example.geoserver.entity.ReportTemplate
import com.example.geoserver.repository.ReportTemplateRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/report-templates")
class ReportTemplateController(
    private val repository: ReportTemplateRepository,
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTemplate(
        @RequestParam name: String,
        @RequestParam(required = false) description: String?,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) metadataJson: String?,
        @RequestParam(defaultValue = "false") overwrite: Boolean // Новый флаг
    ): ResponseEntity<*> {
        val existing = repository.findByName(name)

        if (existing.isPresent && !overwrite) {
            // Если нашли дубликат и нет согласия на перезапись — возвращаем 409
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Шаблон с именем '$name' уже существует.")
        }

        val template = existing.orElse(ReportTemplate(name = name, jrxmlContent = ""))
        
        template.name = name
        template.description = description
        template.jrxmlContent = String(file.bytes, StandardCharsets.UTF_8)
        template.metadataJson = normalizeMetadataJson(metadataJson)

        repository.save(template)
        return ResponseEntity.ok(template.toSummaryDto())
    }

    @GetMapping("/{id}")
    fun getTemplate(@PathVariable id: Long): ReportTemplateDto {
        return repository.findById(id)
            .map { it.toDto() }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
    }


    @DeleteMapping("/{id}")
    fun deleteTemplate(@PathVariable id: Long): ResponseEntity<Unit> {
        if (!repository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        repository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getAll(): List<ReportTemplateSummaryDto> =
        repository.findAll().map { it.toSummaryDto() }

    @GetMapping("/{id}/metadata")
    fun getMetadata(@PathVariable id: Long): ReportTemplateMetadataDto {
        val template = repository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Шаблон не найден") }

        val fromJson = parseMetadata(template.metadataJson)
        return if (fromJson.parameters.isNotEmpty()) fromJson else extractMetadataFromJrxml(template.jrxmlContent)
    }

    private fun extractMetadataFromJrxml(jrxml: String): ReportTemplateMetadataDto {
        val regex = Regex("""<parameter\s+name="([^"]+)"\s+class="([^"]+)"""")
        
        val parameters = regex.findAll(jrxml).map { match ->
            val name = match.groupValues[1]
            val clazz = match.groupValues[2]

            var type = when (clazz) {
                "java.lang.Long", "java.lang.Integer" -> ReportParameterType.NUMBER
                "java.util.Date", "java.time.LocalDate" -> ReportParameterType.DATE
                "java.lang.Boolean" -> ReportParameterType.BOOLEAN
                else -> ReportParameterType.TEXT
            }

            val valueType = when (clazz) {
                "java.lang.Long", "java.lang.Integer" -> ReportValueType.NUMBER
                "java.lang.Double" -> ReportValueType.DECIMAL
                "java.lang.Boolean" -> ReportValueType.BOOLEAN
                else -> ReportValueType.STRING
            }

            if (name.endsWith("Id")) {
                type = ReportParameterType.SELECT
            }

            ReportParameterDto(
                name = name,
                label = humanize(name),
                type = type,
                valueType = valueType
            )
        }
        .distinctBy { it.name } 
        .toList()

        return ReportTemplateMetadataDto(parameters = parameters)
    }

    private fun humanize(name: String): String {
        return name
            .removeSuffix("Id")
            .replaceFirstChar { it.uppercase() }
    }

    private fun normalizeMetadataJson(metadataJson: String?): String? {
        val text = metadataJson?.trim()
        if (text.isNullOrBlank()) return null

        // Проверяем, что JSON валиден
        parseMetadata(text)
        return text
    }

    private fun parseMetadata(metadataJson: String?): ReportTemplateMetadataDto {
        val text = metadataJson?.trim()
        if (text.isNullOrBlank()) return ReportTemplateMetadataDto()

        return try {
            objectMapper.readValue(text)
        } catch (ex: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Некорректный JSON метаданных шаблона: ${ex.message}"
            )
        }
    }

    private fun ReportTemplate.toSummaryDto(): ReportTemplateSummaryDto =
        ReportTemplateSummaryDto(
            id = this.id ?: 0L,
            name = this.name,
            description = this.description
        )

    private fun ReportTemplate.toDto(): ReportTemplateDto =
        ReportTemplateDto(
            id = this.id ?: 0L,
            name = this.name,
            description = this.description,
            jrxmlContent = this.jrxmlContent,
            metadata = parseMetadata(this.metadataJson)
        )

    private fun normalizeTemplateName(raw: String): String {
        return raw.trim()
            .removeSuffix(".jrxml")
            .removeSuffix(".jasper")
    }
    
}