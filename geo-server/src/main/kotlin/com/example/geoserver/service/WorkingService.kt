package com.example.geoserver.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.geoserver.repository.WorkingRepository
import com.example.geoserver.entity.Working

@Service
class WorkingService(private val workingRepository: WorkingRepository) {

    @Transactional
    fun saveWorking(working: Working): Working {
        // Вычисляем корректный флаг isProject перед сохранением
        val withProjectFlag = working.copy(isProject = computeIsProject(working))

        // Если это новая запись (id = 0) и orderNum не задан
        if (withProjectFlag.id == 0L && withProjectFlag.orderNum == null) {
            val maxNum = workingRepository.findMaxOrderNum() ?: 0
            // Присваиваем следующий номер
            return workingRepository.save(withProjectFlag.copy(orderNum = maxNum + 1))
        }
        return workingRepository.save(withProjectFlag)
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
     * Определяет, считать ли выработку проектной.
     *
     * Выработка считается фактической (isProject = false), если выполнены все условия:
     * - заполнен участок (area != null)
     * - заполнен подрядчик (contractor != null)
     * - заполнен геолог (geologist != null)
     * - заполнены фактические координаты actualX, actualY, actualZ, actualDepth (все не null)
     * - дополнительно, если тип выработки равен "скважина" (case-insensitive), то
     *   требуется наличие буровой (drillingRig != null)
     * Во всех остальных случаях выработка считается проектной (isProject = true).
     */
    fun computeIsProject(w: Working): Boolean {
        val hasArea = w.area != null
        val hasContractor = w.contractor != null
        val hasGeologist = w.geologist != null
        val hasActualCoords = w.actualX != null && w.actualY != null && w.actualZ != null && w.actualDepth != null

        val typeName = w.workType?.name?.lowercase()
        val requiresRig = typeName == "скважина"
        val hasRig = w.drillingRig != null

        val isFactual = hasArea && hasContractor && hasGeologist && hasActualCoords && (!requiresRig || hasRig)
        return !isFactual
    }
}