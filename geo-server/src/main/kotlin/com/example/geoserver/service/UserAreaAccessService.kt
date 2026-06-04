package com.example.geoserver.service

import com.example.geoserver.entity.AccessLevel
import com.example.geoserver.entity.UserAreaAccess
import com.example.geoserver.repository.UserAreaAccessRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

@Service
class UserAreaAccessService(
    private val userAreaAccessRepository: UserAreaAccessRepository
) {

    fun getAccessList(userId: Long): List<UserAreaAccess> {
        return userAreaAccessRepository.findAllByUserId(userId)
    }

    fun getAccessForArea(userId: Long, areaId: Long): UserAreaAccess? {
        return userAreaAccessRepository.findByUserIdAndAreaId(userId, areaId)
    }

    fun requireWriteAccess(userId: Long, areaId: Long?) {
        if (areaId == null) {
            throw AccessDeniedException("Access denied: area is not specified")
        }
        val access = getAccessForArea(userId, areaId)
        if (access == null || access.accessLevel != AccessLevel.WRITE) {
            throw AccessDeniedException("Access denied: write access required for area $areaId")
        }
    }
}
