package com.example.geoserver.controller

import com.example.geoserver.security.JwtService
import com.example.geoserver.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val userService: UserService
) {

    data class LoginRequest(val username: String, val password: String)
    // ДОБАВИЛИ ROLE СЮДА
    data class LoginResponse(val token: String, val role: String)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )
            val user = userService.loadUserByUsername(request.username)
            val token = jwtService.generateToken(user)
            val role = user.authorities.firstOrNull()?.authority ?: "ROLE_USER"
            
            return ResponseEntity.ok(LoginResponse(token, role))
        } catch (e: BadCredentialsException) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid credentials"))
        }
    }
}