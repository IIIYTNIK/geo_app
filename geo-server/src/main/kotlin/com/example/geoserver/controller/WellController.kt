package com.example.geoserver.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/wells")
class WellController {

    @GetMapping("/test")
    fun test(auth: Authentication): String {
        return "Привет, ${auth.name}! Ты авторизован как ${auth.authorities}"
    }
}