package org.example.geoapp.util

enum class DbField(val propKey: String, val title: String, val isReference: Boolean = false) {
    IGNORE("ignore", "--- Пропустить ---"),
    
    // Виртуальное поле для склейки
    NAME_COMBINED("nameCombined", "Наименование (Тип + Номер)"), 
    NUMBER("number", "Номер выработки (только номер)"),
    
    // Справочники
    AREA("area", "Участок", true),
    WORK_TYPE("workType", "Тип выработки", true),
    CONTRACTOR("contractor", "Подрядчик", true),
    GEOLOGIST("geologist", "Геолог", true),
    DRILLING_RIG("drillingRig", "Буровая", true),

    // Числа и текст
    PLANNED_X("plannedX", "План X"),
    PLANNED_Y("plannedY", "План Y"),
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
    GW_STABLE_LOG("gwStableLog", "УУГВ устан (журнал)"),
    ACT_NUMBER("actNumber", "Номер акта"),
    ADDITIONAL_INFO("additionalInfo", "Примечание"),

    // Образцы
    SAMPLES_THAWED("samplesThawed", "Образцы талые (шт)"),
    SAMPLES_FROZEN("samplesFrozen", "Образцы мерзлые (шт)"),
    SAMPLES_ROCKY("samplesRocky", "Образцы скальные (шт)")
}

// ВОТ ЭТОТ КЛАСС БЫЛ ПОТЕРЯН:
// Контейнер для строки, требующей исправления
class CorrectionRow(val originalRowIndex: Int, var errorMsg: String, val rawValues: MutableMap<DbField, String>)