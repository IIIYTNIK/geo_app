package com.example.geoserver.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date
import org.slf4j.LoggerFactory

@Service
@Suppress("DEPRECATION")
class JwtService {
    companion object {
        private val log = LoggerFactory.getLogger(JwtService::class.java)
    }

    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 0

    fun generateToken(userDetails: UserDetails): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
        return Jwts.builder()
            .setSubject(userDetails.username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun extractUsername(token: String): String? {
        return try {
            val username = extractClaim(token) { it.subject }
            log.debug("Extracted username from token: $username")
            username
        } catch (e: Exception) {
            log.error("Error extracting username: ${e.message}")
            null
        }
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        val isExpired = isTokenExpired(token)
        val isValid = username == userDetails.username && !isExpired
        log.debug("Token validation: username=$username (expected=${userDetails.username}), expired=$isExpired, valid=$isValid")
        return isValid
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token) { it.expiration }
    }

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
        return io.jsonwebtoken.Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}