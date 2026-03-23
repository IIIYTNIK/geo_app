package org.example.geoapp.util

import com.example.geoapp.api.Working

// Все поля таблицы
enum class DbField(val propKey: String, val title: String) {
    IGNORE("ignore", "--- Пропустить ---"),
    NUMBER("number", "Номер скважины*"),
    PLANNED_X("plannedX", "План X"),
    PLANNED_Y("plannedY", "План Y"),
    PLANNED_Z("plannedZ", "План Z"),
    ACTUAL_X("actualX", "Факт X"),
    ACTUAL_Y("actualY", "Факт Y"),
    ACTUAL_Z("actualZ", "Факт Z"),
    DEPTH("depth", "Глубина"),
    CORE_RECOVERY("coreRecovery", "Керн, %"),
    CASING("casing", "Обсад"),
    START_DATE("startDate", "Дата начала"),
    END_DATE("endDate", "Дата окончания"),
    MMG1_TOP("mmg1Top", "ММГ1 кровля"),
    MMG1_BOTTOM("mmg1Bottom", "ММГ1 подошва"),
    MMG2_TOP("mmg2Top", "ММГ2 кровля"),
    MMG2_BOTTOM("mmg2Bottom", "ММГ2 подошва"),
    GW_APPEAR_LOG("gwAppearLog", "УГВ появ (журнал)"),
    GW_STABLE_LOG("gwStableLog", "УГВ устан (журнал)"),
    GW_STABLE_ABS("gwStableAbs", "УГВ устан (абс)"),
    GW_STABLE_REL("gwStableRel", "УГВ устан (отн)"),
    GW_STABLE_ABS_FINAL("gwStableAbsFinal", "УГВ устан (абс финал)"),
    ACT("act", "Акт"),
    ACT_NUMBER("actNumber", "Номер акта"),
    THERMAL_TUBE("thermalTube", "Термотрубка"),
    ADDITIONAL_INFO("additionalInfo", "Примечание")
}

// Модель строки для окна исправления
class CorrectionRow(val originalRowIndex: Int, var errorMsg: String, val values: MutableMap<DbField, String>)

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