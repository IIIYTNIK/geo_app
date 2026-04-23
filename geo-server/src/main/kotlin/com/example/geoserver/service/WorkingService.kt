package com.example.geoserver.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.geoserver.repository.WorkingRepository
import com.example.geoserver.repository.WorkingSpecifications
import com.example.geoserver.entity.Working
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Service
class WorkingService(private val workingRepository: WorkingRepository) {

    private val logger = LoggerFactory.getLogger(WorkingService::class.java)

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

    /**
     * Фильтрует записи Working по условиям с использованием Specification API.
     *
     * Это решает проблему SQLState: 42P18 в PostgreSQL:
     * - Динамически добавляет условия только для non-null параметров
     * - Избегает конструкции ":param IS NULL"
     * - PostgreSQL получает явно типизированные параметры
     *
     * @param startDate начальная дата (если null, фильтр не применяется)
     * @param endDate конечная дата (если null, фильтр не применяется)
     * @param contractorId ID подрядчика (если null, фильтр не применяется)
     * @param areaId ID участка (если null, фильтр не применяется)
     * @return список Working, соответствующих фильтрам
     */
    @Transactional(readOnly = true)
    fun filterWorkings(
        startDate: LocalDate?,
        endDate: LocalDate?,
        contractorId: Long?,
        areaId: Long?
    ): List<Working> {
        logger.info("Starting filter operation with parameters: startDate=$startDate, endDate=$endDate, contractorId=$contractorId, areaId=$areaId")
        
        val specification = WorkingSpecifications.filterByParameters(startDate, endDate, contractorId, areaId)
        val result = workingRepository.findAll(specification)
        
        logger.info("Filter operation completed. Found ${result.size} records")
        return result
    }
}