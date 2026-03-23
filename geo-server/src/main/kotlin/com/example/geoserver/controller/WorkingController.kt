package com.example.geoserver.controller

/// Контроллер для управления записями о скважинах, шурфах и расчистках (Workings).

import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import com.example.geoserver.entity.*
import com.example.geoserver.repository.*

@RestController
@RequestMapping("/api/workings")
class WorkingController(private val repo: WorkingRepository) {

    @GetMapping
    fun getAll(): List<Working> = repo.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Working? = repo.findById(id).orElse(null)

    @PostMapping
    fun create(@RequestBody working: Working): Working = repo.save(working)

    @PostMapping("/batch")
    fun createBatch(@RequestBody workings: List<Working>): List<Working> {
        val savedWorkings = mutableListOf<Working>()
        for (w in workings) {
            val existing = repo.findByNumber(w.number)
            if (existing != null) {
                // Если номер уже есть, обновляем существующую запись
                savedWorkings.add(repo.save(w.copy(id = existing.id)))
            } else {
                // Иначе создаем новую
                savedWorkings.add(repo.save(w))
            }
        }
        return savedWorkings
    }
    
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody working: Working): Working {
        if (repo.existsById(id)) {
            return repo.save(working.copy(id = id))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Working not found")
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        repo.deleteById(id)
    }
}