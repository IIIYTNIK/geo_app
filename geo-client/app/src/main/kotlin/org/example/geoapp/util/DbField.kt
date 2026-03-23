package org.example.geoapp.util

import com.example.geoapp.api.*

enum class DbField(val propKey: String, val title: String, val isReference: Boolean = false) {
    IGNORE("ignore", "--- Пропустить ---"),
    NUMBER("number", "Номер скважины*"),
    
    // Справочники
    AREA("area", "Участок", true),
    WORK_TYPE("workType", "Тип выработки", true),
    CONTRACTOR("contractor", "Подрядчик", true),
    GEOLOGIST("geologist", "Геолог", true),
    DRILLING_RIG("drillingRig", "Буровая", true),

    // Числа и текст
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

// Контейнер для строки, требующей исправления
class CorrectionRow(val originalRowIndex: Int, var errorMsg: String, val rawValues: MutableMap<DbField, String>)