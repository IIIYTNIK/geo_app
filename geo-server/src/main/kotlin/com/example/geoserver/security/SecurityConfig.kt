package com.example.geoserver.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.example.geoserver.security.JwtFilter
import com.example.geoserver.service.UserService

@Configuration
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val userService: UserService
) {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    @Suppress("DEPRECATION")
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/hello").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/references/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers("/api/users/**").authenticated()
                    .requestMatchers("/api/workings/**").authenticated()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}