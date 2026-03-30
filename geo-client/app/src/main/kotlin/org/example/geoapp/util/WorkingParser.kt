package org.example.geoapp.util

import com.example.geoapp.api.Working
import com.example.geoapp.api.RefWorkType

object WorkingParser {
    // Передаем список доступных типов выработок, чтобы парсер мог найти совпадение
    fun parse(values: Map<DbField, String>, availableWorkTypes: List<RefWorkType> = emptyList()): Working {
        fun getNum(field: DbField): Double? {
            val str = values[field]?.replace(",", ".")?.trim()
            if (str.isNullOrEmpty()) return null
            return str.toDoubleOrNull() ?: throw Exception("Ожидается число для '${field.title}', найдено '$str'")
        }

        fun getInt(field: DbField): Int? {
            val str = values[field]?.trim()
            if (str.isNullOrEmpty()) return null
            return str.toIntOrNull() ?: throw Exception("Ожидается целое число для '${field.title}', найдено '$str'")
        }

        fun getStr(field: DbField): String? {
            val str = values[field]?.trim()
            return if (str.isNullOrEmpty()) null else str
        }

        // Логика извлечения Номера и Типа выработки
        var parsedNumber = getStr(DbField.NUMBER) ?: ""
        var parsedWorkType: RefWorkType? = null

        val combinedName = getStr(DbField.NAME_COMBINED)
        if (combinedName != null) {
            // Регулярное выражение: ищем место, где начинаются цифры. 
            // Например "Скв-12а" разобьется на "Скв-" и "12а"
            val matchResult = Regex("^([^\\d]*)([\\d]+.*)$").find(combinedName)
            if (matchResult != null) {
                val typePrefix = matchResult.groupValues[1].replace("-", "").trim()
                parsedNumber = matchResult.groupValues[2].trim()

                // Пытаемся сопоставить префикс с реальным справочником (например, "Скв" -> "Скважина")
                if (typePrefix.isNotEmpty()) {
                    parsedWorkType = availableWorkTypes.find { 
                        it.name.contains(typePrefix, ignoreCase = true) || typePrefix.contains(it.name, ignoreCase = true)
                    }
                }
            } else {
                // Если цифр нет, просто пишем все в номер
                parsedNumber = combinedName
            }
        }

        if (parsedNumber.isEmpty()) {
            throw Exception("Обязательное поле 'Номер' не может быть пустым")
        }

        val coreRec = getNum(DbField.CORE_RECOVERY)
        if (coreRec != null && (coreRec < 0 || coreRec > 100)) {
            throw Exception("Выход керна должен быть от 0 до 100")
        }

        return Working(
            number = parsedNumber,
            workType = parsedWorkType, // Это поле потом дополнится в ExcelImportController, если тут null
            plannedX = getNum(DbField.PLANNED_X),
            plannedY = getNum(DbField.PLANNED_Y),
            actualX = getNum(DbField.ACTUAL_X),
            actualY = getNum(DbField.ACTUAL_Y),
            actualZ = getNum(DbField.ACTUAL_Z),
            depth = getNum(DbField.DEPTH),
            coreRecovery = coreRec,
            casing = getNum(DbField.CASING),
            startDate = getStr(DbField.START_DATE),
            endDate = getStr(DbField.END_DATE),
            mmg1Top = getNum(DbField.MMG1_TOP),
            mmg1Bottom = getNum(DbField.MMG1_BOTTOM),
            mmg2Top = getNum(DbField.MMG2_TOP),
            mmg2Bottom = getNum(DbField.MMG2_BOTTOM),
            gwAppearLog = getNum(DbField.GW_APPEAR_LOG),
            gwStableLog = getNum(DbField.GW_STABLE_LOG),
            actNumber = getStr(DbField.ACT_NUMBER),
            additionalInfo = getStr(DbField.ADDITIONAL_INFO),
            samplesThawed = getInt(DbField.SAMPLES_THAWED),
            samplesFrozen = getInt(DbField.SAMPLES_FROZEN),
            samplesRocky = getInt(DbField.SAMPLES_ROCKY)
        )
    }
}