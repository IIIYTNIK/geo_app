package com.example.geoserver.security

import com.example.geoserver.entity.User
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun currentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return authentication.principal as? User
    }

    fun currentUserId(): Long? = currentUser()?.id
}
