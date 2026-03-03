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