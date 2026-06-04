package com.example.geoserver.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.example.geoserver.service.UserService
import org.slf4j.LoggerFactory

@Component
class JwtFilter(
    private val jwtService: JwtService,
    private val userService: UserService
) : OncePerRequestFilter() {
    companion object {
        private val log = LoggerFactory.getLogger(JwtFilter::class.java)
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val path = request.requestURI
        val method = request.method
        val authHeader = request.getHeader("Authorization")
        
        log.debug("JwtFilter: $method $path, Auth header: ${if (authHeader != null) "present" else "missing"}")
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token for $method $path")
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7).trim()
        if (jwt.isEmpty() || jwt.equals("null", ignoreCase = true)) {
            log.warn("Empty or null JWT token for $method $path")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is missing")
            return
        }

        try {
            log.debug("Attempting to extract username from token for $method $path")
            val username = jwtService.extractUsername(jwt)
            log.debug("Extracted username: $username")

            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userService.loadUserByUsername(username)
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.debug("Token valid for user: $username")
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                } else {
                    log.warn("Token invalid for user: $username")
                }
            }
        } catch (ex: io.jsonwebtoken.JwtException) {
            log.error("JWT parsing error: ${ex.message}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token")
            return
        } catch (ex: Exception) {
            log.error("Authentication error: ${ex.message}", ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication error")
            return
        }

        filterChain.doFilter(request, response)
    }
}