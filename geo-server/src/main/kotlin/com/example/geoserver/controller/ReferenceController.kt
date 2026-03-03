package com.example.geoserver.controller

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping

import com.example.geoserver.entity.*
import com.example.geoserver.repository.*

@RestController
@RequestMapping("/api/references")
class ReferenceController(
    private val contractorRepo: RefContractorRepository,
    private val areaRepo: RefAreaRepository,
    private val geologistRepo: RefGeologistRepository,
    private val drillingRigRepo: RefDrillingRigRepository,
    private val workTypeRepo: RefWorkTypeRepository
) {

    @GetMapping("/contractors")
    fun getContractors(): List<RefContractor> = contractorRepo.findAll()

    @GetMapping("/areas")
    fun getAreas(): List<RefArea> = areaRepo.findAll()

    @GetMapping("/geologists")
    fun getGeologists(): List<RefGeologist> = geologistRepo.findAll()

    @GetMapping("/drilling-rigs")
    fun getDrillingRigs(): List<RefDrillingRig> = drillingRigRepo.findAll()

    @GetMapping("/work-types")
    fun getWorkTypes(): List<RefWorkType> = workTypeRepo.findAll()
}