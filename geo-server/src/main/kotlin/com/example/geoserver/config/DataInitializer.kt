package com.example.geoserver.config

import com.example.geoserver.entity.*
import com.example.geoserver.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jdbcTemplate: JdbcTemplate,

    private val contractorRepo: RefContractorRepository,
    private val areaRepo: RefAreaRepository,
    private val geologistRepo: RefGeologistRepository,
    private val drillingRigRepo: RefDrillingRigRepository,
    private val workTypeRepo: RefWorkTypeRepository,
    private val workingRepo: WorkingRepository
) {

    @Bean
    fun initData(): CommandLineRunner = CommandLineRunner {
        try {
            jdbcTemplate.execute(
                """
                    UPDATE users
                    SET login = COALESCE(NULLIF(login, ''), username),
                        full_name = COALESCE(NULLIF(full_name, ''), username)
                    WHERE username IS NOT NULL
                """.trimIndent()
            )
        } catch (ignored: Exception) {
            // Если старой колонки username нет, пропускаем миграцию.
        }

        if (userRepository.findByLogin("admin").isEmpty) {
            val admin = User(
                login = "admin",
                fullName = "Администратор",
                password = passwordEncoder.encode("secret123"),
                role = "ROLE_ADMIN"
            )
            userRepository.save(admin)
            // println("Создан тестовый пользователь: admin / secret123")
        }
        if (userRepository.findByLogin("user").isEmpty) {
            val user = User(
                login = "user",
                fullName = "Пользователь",
                password = passwordEncoder.encode("password"),
                role = "ROLE_USER"
            )
            userRepository.save(user)
        }
        println("Тестовые пользователи созданы")
    }
}