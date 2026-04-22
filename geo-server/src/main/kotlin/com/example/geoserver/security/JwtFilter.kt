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
    
    private val logger = LoggerFactory.getLogger(JwtFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authHeader = request.getHeader("Authorization")
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No valid Authorization header: {}", authHeader)
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7).trim()
        if (jwt.isEmpty() || jwt.equals("null", ignoreCase = true)) {
            logger.warn("JWT token is empty or null")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is missing")
            return
        }

        try {
            val username = jwtService.extractUsername(jwt)
            logger.debug("Extracted username from JWT: {}", username)

            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userService.loadUserByUsername(username)
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    logger.debug("Token is valid for user: {}", username)
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                    logger.debug("Authentication set in SecurityContext for user: {}", username)
                } else {
                    logger.warn("Token is invalid for user: {}", username)
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token")
                    return
                }
            }
        } catch (ex: io.jsonwebtoken.JwtException) {
            logger.error("JWT exception: {}", ex.message, ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token: ${ex.message}")
            return
        } catch (ex: Exception) {
            logger.error("Authentication error: {}", ex.message, ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication error: ${ex.message}")
            return
        }

        filterChain.doFilter(request, response)
    }
}