package org.example.geoapp.util

import com.example.geoapp.api.Working

object WorkingParser {
    fun parse(values: Map<DbField, String>): Working {
        fun getNum(field: DbField): Double? {
            val str = values[field]?.replace(",", ".")?.trim()
            if (str.isNullOrEmpty()) return null
            return str.toDoubleOrNull() ?: throw Exception("Ожидается число для '${field.title}', найдено '$str'")
        }

        fun getStr(field: DbField): String? {
            val str = values[field]?.trim()
            return if (str.isNullOrEmpty()) null else str
        }

        val number = getStr(DbField.NUMBER) ?: throw Exception("Обязательное поле 'Номер скважины*' не может быть пустым")

        val coreRec = getNum(DbField.CORE_RECOVERY)
        if (coreRec != null && (coreRec < 0 || coreRec > 100)) {
            throw Exception("Выход керна должен быть от 0 до 100")
        }

        return Working(
            number = number,
            plannedX = getNum(DbField.PLANNED_X),
            plannedY = getNum(DbField.PLANNED_Y),
            plannedZ = getNum(DbField.PLANNED_Z),
            actualX = getNum(DbField.ACTUAL_X),
            actualY = getNum(DbField.ACTUAL_Y),
            actualZ = getNum(DbField.ACTUAL_Z),
            depth = getNum(DbField.DEPTH),
            coreRecovery = coreRec,
            casing = getStr(DbField.CASING),
            startDate = getStr(DbField.START_DATE),
            endDate = getStr(DbField.END_DATE),
            mmg1Top = getNum(DbField.MMG1_TOP),
            mmg1Bottom = getNum(DbField.MMG1_BOTTOM),
            mmg2Top = getNum(DbField.MMG2_TOP),
            mmg2Bottom = getNum(DbField.MMG2_BOTTOM),
            gwAppearLog = getNum(DbField.GW_APPEAR_LOG),
            gwStableLog = getNum(DbField.GW_STABLE_LOG),
            gwStableAbs = getNum(DbField.GW_STABLE_ABS),
            gwStableRel = getNum(DbField.GW_STABLE_REL),
            gwStableAbsFinal = getNum(DbField.GW_STABLE_ABS_FINAL),
            act = getStr(DbField.ACT),
            actNumber = getStr(DbField.ACT_NUMBER),
            thermalTube = getStr(DbField.THERMAL_TUBE),
            additionalInfo = getStr(DbField.ADDITIONAL_INFO)
        )
    }
}