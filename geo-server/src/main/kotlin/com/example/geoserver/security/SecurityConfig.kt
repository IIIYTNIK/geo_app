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
import com.example.geoserver.service.UserService
import org.springframework.http.HttpMethod

@Configuration
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val userService: UserService
) {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
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
                it.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                
                it.requestMatchers(HttpMethod.GET, "/api/references/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/references/**").hasAuthority("ROLE_ADMIN")
                it.requestMatchers(HttpMethod.PUT, "/api/references/**").hasAuthority("ROLE_ADMIN")
                it.requestMatchers(HttpMethod.DELETE, "/api/references/**").hasAuthority("ROLE_ADMIN")
                
                it.requestMatchers("/api/users/**").authenticated()
                it.requestMatchers("/api/workings/**").authenticated()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}