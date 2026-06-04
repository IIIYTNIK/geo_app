package com.example.geoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder

@SpringBootApplication
@EnableJpaAuditing
class GeoServerApplication {

    @Bean
    fun auditorProvider(): AuditorAware<String> = AuditorAware {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication.name == "anonymousUser") {
            java.util.Optional.empty()
        } else {
            java.util.Optional.of(authentication.name)
        }
    }
}

fun main(args: Array<String>) {
	runApplication<GeoServerApplication>(*args)
}
