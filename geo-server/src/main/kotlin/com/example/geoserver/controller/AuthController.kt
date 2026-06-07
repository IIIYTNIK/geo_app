package com.example.geoserver.controller

import com.example.geoserver.security.JwtService
import com.example.geoserver.service.UserService
import com.example.geoserver.controller.UserDto
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

    data class LoginRequest(
        val login: String,
        val password: String
    )
    data class LoginResponse(val token: String, val role: String, val user: UserDto)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    request.login,
                    request.password
                )
            )

            val userDetails = userService.loadUserByUsername(request.login)

            val token = jwtService.generateToken(userDetails)

            val role = userDetails.authorities
                .firstOrNull()
                ?.authority ?: "ROLE_USER"

            val dbUser = userService.loadUserByUsername(request.login) as com.example.geoserver.entity.User

            val userDto = UserDto(
                id = dbUser.id,
                login = dbUser.username,
                fullName = dbUser.fullName,
                role = role,
                position = dbUser.position
            )

            return ResponseEntity.ok(
                LoginResponse(
                    token = token,
                    role = role,
                    user = userDto
                )
            )
        } catch (e: BadCredentialsException) {
            return ResponseEntity
                .status(401)
                .body(mapOf("error" to "Invalid credentials"))
        }
    }
}