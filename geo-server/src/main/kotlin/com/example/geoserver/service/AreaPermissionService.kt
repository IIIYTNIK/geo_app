package com.example.geoserver.service

import com.example.geoserver.entity.AccessLevel
import com.example.geoserver.repository.UserAreaAccessRepository
import com.example.geoserver.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AreaPermissionService(
    private val userAreaAccessRepository: UserAreaAccessRepository,
    private val userRepository: UserRepository
) {

    fun canRead(username: String, areaId: Long?): Boolean {
        if (areaId == null) return false
        val userId = userRepository.findByLogin(username).get().id
        val access = userAreaAccessRepository.findByUser_IdAndArea_Id(userId, areaId)
        return access != null // имеет любой доступ (READ или WRITE)
    }

    fun canWrite(username: String, areaId: Long?): Boolean {
        if (areaId == null) return true // Если участок не задан, разрешаем запись (для новых записей)
        val userId = userRepository.findByLogin(username).get().id
        val access = userAreaAccessRepository.findByUser_IdAndArea_Id(userId, areaId)
        return access != null && access.accessLevel == AccessLevel.WRITE
    }
}