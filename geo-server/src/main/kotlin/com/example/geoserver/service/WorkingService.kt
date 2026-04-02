package com.example.geoserver.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.geoserver.repository.WorkingRepository
import com.example.geoserver.entity.Working

@Service
class WorkingService(private val workingRepository: WorkingRepository) {

    @Transactional
    fun saveWorking(working: Working): Working {
        // Если это новая запись (id = 0) и orderNum не задан
        if (working.id == 0L && working.orderNum == null) {
            val maxNum = workingRepository.findMaxOrderNum() ?: 0
            // Присваиваем следующий номер
            return workingRepository.save(working.copy(orderNum = maxNum + 1))
        }
        return workingRepository.save(working)
    }

    @Transactional
    fun deleteWorking(id: Long) {
        val working = workingRepository.findById(id).orElse(null) ?: return
        val deletedOrderNum = working.orderNum
        
        // Удаляем запись
        workingRepository.deleteById(id)

        // Если у удаленной записи был порядковый номер, закрываем "дырку"
        if (deletedOrderNum != null) {
            workingRepository.shiftOrderNums(deletedOrderNum)
        }
    }
}