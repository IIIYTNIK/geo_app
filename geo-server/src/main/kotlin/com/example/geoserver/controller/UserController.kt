package com.example.geoserver.controller

import com.example.geoserver.entity.User
import com.example.geoserver.repository.UserRepository
import com.example.geoserver.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private var userRepository: UserRepository,
    private val userService: UserService,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    @GetMapping
    fun getAllUsers(): List<UserDto> {
        return userRepository.findAll().map { UserDto(it.id, it.login, it.fullName, it.role, it.position) }
    }

    @PostMapping
    fun createUser(@RequestBody req: UserCreateDto): ResponseEntity<Any> {
        if (userRepository.findByLogin(req.login).isPresent) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Пользователь уже существует"))
        }
        val user = userService.createUser(
            login = req.login,
            fullName = req.fullName,
            rawPassword = req.password,
            role = req.role.removePrefix("ROLE_"), // UserService сам добавляет ROLE_
            position = req.position?.ifBlank { null }
        )
        return ResponseEntity.ok(UserDto(user.id, user.login, user.fullName, user.role, user.position))
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody req: UserUpdateDto): ResponseEntity<Any> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }

        user.updateLogin(req.login)
        user.updateFullName(req.fullName)
        user.role = if (req.role.startsWith("ROLE_")) req.role else "ROLE_${req.role}"
        user.position = req.position?.ifBlank { null }
        if (!req.password.isNullOrBlank()) {
            user.updatePassword(passwordEncoder.encode(req.password))
        }
        userRepository.save(user)

        return ResponseEntity.ok(UserDto(user.id, user.login, user.fullName, user.role, user.position))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        userRepository.deleteById(id)
        return ResponseEntity.ok().build()
    }
}

// DTO классы для контроллера
data class UserDto(val id: Long, val login: String, val fullName: String, val role: String, val position: String?)
data class UserCreateDto(val login: String, val fullName: String, val password: String, val role: String, val position: String?)
data class UserUpdateDto(val login: String, val fullName: String, val role: String, val password: String?, val position: String?)