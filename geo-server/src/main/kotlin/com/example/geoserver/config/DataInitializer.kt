package com.example.geoserver.config

import com.example.geoserver.entity.*
import com.example.geoserver.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDate

@Configuration
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,

    private val contractorRepo: RefContractorRepository,
    private val areaRepo: RefAreaRepository,
    private val geologistRepo: RefGeologistRepository,
    private val drillingRigRepo: RefDrillingRigRepository,
    private val workTypeRepo: RefWorkTypeRepository,
    private val workingRepo: WorkingRepository
) {

    @Bean
    fun initData(): CommandLineRunner = CommandLineRunner {
        if (userRepository.findByUsername("admin").isEmpty) {
            val admin = User(
                username = "admin",
                password = passwordEncoder.encode("secret123"),
                role = "ROLE_ADMIN"
            )
            userRepository.save(admin)
            // println("Создан тестовый пользователь: admin / secret123")
        }
        if (userRepository.findByUsername("user").isEmpty) {
            val user = User(
                username = "user",
                password = passwordEncoder.encode("password"),
                role = "ROLE_USER"
            )
            userRepository.save(user)
        }
        println("Тестовые пользователи созданы")
    }
}