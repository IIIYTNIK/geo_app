package com.example.geoserver.controller

import com.example.geoserver.entity.User
import com.example.geoserver.entity.AccessLevel
import com.example.geoserver.entity.UserAreaAccess
import com.example.geoserver.repository.UserRepository
import com.example.geoserver.repository.UserAreaAccessRepository
import com.example.geoserver.repository.RefAreaRepository
import com.example.geoserver.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private var userRepository: UserRepository,
    private val userService: UserService,
    private val userAreaAccessRepository: UserAreaAccessRepository,
    private val refAreaRepository: RefAreaRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    @GetMapping
    fun getAllUsers(): List<UserDto> {
        return userRepository.findAll().map { UserDto(it.id, it.username, it.fullName, it.role, it.position) }
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
        return ResponseEntity.ok(UserDto(user.id, user.username, user.fullName, user.role, user.position))
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

        return ResponseEntity.ok(UserDto(user.id, user.username, user.fullName, user.role, user.position))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        userRepository.deleteById(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me/access")
    fun getMyAccess(): List<UserAreaAccessDto> {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name
        val user = userRepository.findByLogin(username).orElseThrow()
        val accessList = userAreaAccessRepository.findByUser_Id(user.id)
        return accessList.map { UserAreaAccessDto(it.area.id, it.accessLevel) }
    }

    @GetMapping("/access/all")
    fun getAllUsersAccess(): List<UserAreaAccessDto> {
        return userAreaAccessRepository.findAll().map { 
            UserAreaAccessDto(it.area.id, it.accessLevel, it.user.id) 
        }
    }

    @PutMapping("/{userId}/access/{areaId}")
    fun updateUserAccess(
        @PathVariable userId: Long, 
        @PathVariable areaId: Long, 
        @RequestParam level: String
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        val area = refAreaRepository.findById(areaId).orElseThrow { RuntimeException("Area not found") }
        
        val existingAccess = userAreaAccessRepository.findByUser_IdAndArea_Id(userId, areaId)

        if (level == "NONE") {
            // Если выбрали "Нет доступа", просто удаляем запись
            existingAccess?.let { userAreaAccessRepository.delete(it) }
        } else {
            // Если дали права READ или WRITE
            val newLevel = AccessLevel.valueOf(level)
            if (existingAccess != null) {
                userAreaAccessRepository.delete(existingAccess)
            }
            userAreaAccessRepository.save(UserAreaAccess(user = user, area = area, accessLevel = newLevel))
        }
        return ResponseEntity.ok().build()
    }
}

data class UserAreaAccessDto(val areaId: Long, val accessLevel: AccessLevel, val userId: Long? = null)
data class UserDto(val id: Long, val login: String, val fullName: String, val role: String, val position: String?)
data class UserCreateDto(val login: String, val fullName: String, val password: String, val role: String, val position: String?)
data class UserUpdateDto(val login: String, val fullName: String, val role: String, val password: String?, val position: String?)