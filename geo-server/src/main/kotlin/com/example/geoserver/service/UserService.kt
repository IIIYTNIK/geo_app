package com.example.geoserver.service

import com.example.geoserver.entity.User
import com.example.geoserver.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.context.annotation.Lazy

@Service
class UserService(
    private val userRepository: UserRepository,
    @Lazy private val passwordEncoder: BCryptPasswordEncoder
) : UserDetailsService {

    override fun loadUserByUsername(login: String): UserDetails {
        return userRepository.findByLogin(login)
            .orElseThrow { UsernameNotFoundException("User not found: $login") }
    }

    // Метод для создания пользователя
    fun createUser(login: String, fullName: String, rawPassword: String, role: String, position: String?): User {
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val user = User(
            login = login,
            fullName = fullName.trim(),
            password = encodedPassword,
            role = "ROLE_$role",  // Spring Security требует префикс ROLE_
            position = position?.ifEmpty { null }
        )
        return userRepository.save(user)
    }
}