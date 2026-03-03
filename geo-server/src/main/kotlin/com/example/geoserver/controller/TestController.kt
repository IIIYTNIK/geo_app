package com.example.geoserver.controller

import com.example.geoserver.entity.User
import com.example.geoserver.repository.UserRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
class TestController(
    private val userRepository: UserRepository,
    private val handlerMapping: org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
) {

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello from Geo Server! База подключена."
    }

    @GetMapping
    fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    // Debug endpoint to list all registered request mappings (temporary)
    @GetMapping("/_debug/mappings")
    fun mappings(): List<String> {
        return handlerMapping.handlerMethods.keys.flatMap { it.patternsCondition?.patterns ?: emptySet() }
    }
}

