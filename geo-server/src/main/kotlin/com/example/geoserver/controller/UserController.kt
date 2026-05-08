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
        return userRepository.findAll().map { UserDto(it.id, it.username, it.role, it.position) }
    }

    @PostMapping
    fun createUser(@RequestBody req: UserCreateDto): ResponseEntity<Any> {
        if (userRepository.findByUsername(req.username).isPresent) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Пользователь уже существует"))
        }
        val user = userService.createUser(
            username = req.username,
            rawPassword = req.password,
            role = req.role.removePrefix("ROLE_"), // UserService сам добавляет ROLE_
            position = req.position ?: null
        )
        return ResponseEntity.ok(UserDto(user.id, user.username, user.role, user.position))
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody req: UserUpdateDto): ResponseEntity<Any> {
        var user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }
        
        // ВАЖНО: Для этого поля в классе User должны быть var!
        user.updateUsername(req.username)
        user.role = if (req.role.startsWith("ROLE_")) req.role else "ROLE_${req.role}"
        user.position = req.position ?: null
        if (!req.password.isNullOrBlank()) {
            user.updatePassword(passwordEncoder.encode(req.password))
        }
        userRepository.save(user)
        
        return ResponseEntity.ok(UserDto(user.id, user.username, user.role, user.position))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        userRepository.deleteById(id)
        return ResponseEntity.ok().build()
    }
}

// DTO классы для контроллера
data class UserDto(val id: Long, val username: String, val role: String, val position: String?)
data class UserCreateDto(val username: String, val password: String, val role: String, val position: String?)
data class UserUpdateDto(val username: String, val role: String, val password: String?, val position: String?)