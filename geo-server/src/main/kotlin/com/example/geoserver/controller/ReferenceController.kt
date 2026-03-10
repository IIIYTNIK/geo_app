package com.example.geoserver.controller

import org.springframework.web.bind.annotation.*
import com.example.geoserver.entity.*
import com.example.geoserver.repository.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/references")
class ReferenceController(
    private val contractorRepo: RefContractorRepository,
    private val areaRepo: RefAreaRepository,
    private val geologistRepo: RefGeologistRepository,
    private val drillingRigRepo: RefDrillingRigRepository,
    private val workTypeRepo: RefWorkTypeRepository
) {
    // --- УЧАСТКИ ---
    @GetMapping("/areas") fun getAreas() = areaRepo.findAll()
    @PostMapping("/areas") fun createArea(@RequestBody item: RefArea) = areaRepo.save(item)
    @PutMapping("/areas/{id}") fun updateArea(@PathVariable id: Long, @RequestBody item: RefArea): RefArea {
        if (!areaRepo.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return areaRepo.save(item.copy(id = id))
    }
    @DeleteMapping("/areas/{id}") fun deleteArea(@PathVariable id: Long) = areaRepo.deleteById(id)

    // --- ТИПЫ ВЫРАБОТОК ---
    @GetMapping("/work-types") fun getWorkTypes() = workTypeRepo.findAll()
    @PostMapping("/work-types") fun createWorkType(@RequestBody item: RefWorkType) = workTypeRepo.save(item)
    @PutMapping("/work-types/{id}") fun updateWorkType(@PathVariable id: Long, @RequestBody item: RefWorkType): RefWorkType {
        if (!workTypeRepo.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return workTypeRepo.save(item.copy(id = id))
    }
    @DeleteMapping("/work-types/{id}") fun deleteWorkType(@PathVariable id: Long) = workTypeRepo.deleteById(id)

    // --- БУРОВЫЕ ---
    @GetMapping("/drilling-rigs") fun getDrillingRigs() = drillingRigRepo.findAll()
    @PostMapping("/drilling-rigs") fun createDrillingRig(@RequestBody item: RefDrillingRig) = drillingRigRepo.save(item)
    @PutMapping("/drilling-rigs/{id}") fun updateDrillingRig(@PathVariable id: Long, @RequestBody item: RefDrillingRig): RefDrillingRig {
        if (!drillingRigRepo.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return drillingRigRepo.save(item.copy(id = id))
    }
    @DeleteMapping("/drilling-rigs/{id}") fun deleteDrillingRig(@PathVariable id: Long) = drillingRigRepo.deleteById(id)

    // --- ПОДРЯДЧИКИ ---
    @GetMapping("/contractors") fun getContractors() = contractorRepo.findAll()
    @PostMapping("/contractors") fun createContractor(@RequestBody item: RefContractor) = contractorRepo.save(item)
    @PutMapping("/contractors/{id}") fun updateContractor(@PathVariable id: Long, @RequestBody item: RefContractor): RefContractor {
        if (!contractorRepo.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return contractorRepo.save(item.copy(id = id))
    }
    @DeleteMapping("/contractors/{id}") fun deleteContractor(@PathVariable id: Long) = contractorRepo.deleteById(id)

    // --- ГЕОЛОГИ ---
    @GetMapping("/geologists") fun getGeologists() = geologistRepo.findAll()
    @PostMapping("/geologists") fun createGeologist(@RequestBody item: RefGeologist) = geologistRepo.save(item)
    @PutMapping("/geologists/{id}") fun updateGeologist(@PathVariable id: Long, @RequestBody item: RefGeologist): RefGeologist {
        if (!geologistRepo.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        item.contractor = contractorRepo.findById(item.contractor?.id ?: 0).orElse(null)
        return geologistRepo.save(item.copy(id = id))
    }
    @DeleteMapping("/geologists/{id}") fun deleteGeologist(@PathVariable id: Long) = geologistRepo.deleteById(id)

    @GetMapping("/geologists/by-contractor/{contractorId}")
    fun getGeologistsByContractor(@PathVariable contractorId: Long) = geologistRepo.findByContractorId(contractorId)
}