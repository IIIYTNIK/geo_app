package com.example.geoserver.controller

import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import com.example.geoserver.entity.*
import com.example.geoserver.repository.*
import com.example.geoserver.service.WorkingService
import kotlin.math.sqrt
import kotlin.math.round


@RestController
@RequestMapping("/api/workings")
class WorkingController(
    private val repo: WorkingRepository,
    private val workingService: WorkingService
) {

    @GetMapping
    fun getAll(): List<Working> = repo.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Working? = repo.findById(id).orElse(null)

    @PostMapping
    fun create(@RequestBody working: Working) = workingService.saveWorking(working)

    @PostMapping("/batch")
    fun createBatch(@RequestBody workings: List<Working>): List<Working> {
        val savedWorkings = mutableListOf<Working>()
        for (w in workings) {
            val processedWorking = calculateComputedFields(w)
            // Проверка на существование по номеру и участку (как на клиенте)
            val existing = repo.findByNumberAndAreaId(processedWorking.number, processedWorking.area?.id)
            if (existing != null) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate: ${processedWorking.number} on area ${processedWorking.area?.name}")
            }
            savedWorkings.add(repo.save(processedWorking))
        }
        return savedWorkings
    }
    
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody working: Working): Working {
        if (repo.existsById(id)) {
            return repo.save(calculateComputedFields(working).copy(id = id))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Working not found")
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        repo.deleteById(id)
    }

    // ЛОГИКА РАСЧЕТОВ И ОКРУГЛЕНИЙ 
    private fun calculateComputedFields(w: Working): Working {
        // Округление до 3 знаков
        fun round3(value: Double?): Double? {
            return value?.let { round(it * 1000.0) / 1000.0 }
        }

        val px = round3(w.plannedX)
        val py = round3(w.plannedY)
        val ax = round3(w.actualX)
        val ay = round3(w.actualY)
        val az = round3(w.actualZ)

        // Расчет Смещения (Delta S)
        var dS: Double? = null
        if (px != null && py != null && ax != null && ay != null) {
            val dx = px - ax
            val dy = py - ay
            dS = round3(sqrt(dx * dx + dy * dy))
        }

        // Расчет УУГВ абс (ФактZ - УУГВ устан.)
        var gwAbs: Double? = null
        val stableLog = w.gwStableLog
        if (az != null && stableLog != null) {
            gwAbs = round3(az - stableLog)
        }

        return w.copy(
            plannedX = px,
            plannedY = py,
            actualX = ax,
            actualY = ay,
            actualZ = az,
            deltaS = dS,
            gwStableAbs = gwAbs
        )
    }
}