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

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }
    }

    // Метод для создания пользователя
    fun createUser(username: String, rawPassword: String, role: String): User {
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val user = User(
            username = username,
            password = encodedPassword,
            role = "ROLE_$role"  // Spring Security требует префикс ROLE_
        )
        return userRepository.save(user)
    }
}