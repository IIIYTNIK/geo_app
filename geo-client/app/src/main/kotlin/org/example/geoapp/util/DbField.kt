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
    STRUCTURE("structure", "Сооружение"),
    PLANNED_CONTRACTOR("plannedContractor", "Запланированный подрядчик", true),

    // Числа и текст
    PLANNED_X("plannedX", "План X"),
    PLANNED_Y("plannedY", "План Y"),
    PLANNED_DEPTH("plannedDepth", "План H"),
    ACTUAL_X("actualX", "Факт X"),
    ACTUAL_Y("actualY", "Факт Y"),
    ACTUAL_Z("actualZ", "Факт Z"),
    ACTUAL_DEPTH("actualDepth", "Факт H"),
    CORE_RECOVERY("coreRecovery", "Выход керна, %"),
    CASING("casing", "Обсад"),
    START_DATE("startDate", "Дата начала"),
    END_DATE("endDate", "Дата окончания"),
    MMG1_TOP("mmg1Top", "ММГ1 кр."),
    MMG1_BOTTOM("mmg1Bottom", "ММГ1 под."),
    MMG2_TOP("mmg2Top", "ММГ2 кр."),
    MMG2_BOTTOM("mmg2Bottom", "ММГ2 под."),
    GW_APPEAR_LOG("gwAppearLog", "ПУГВ"),
    GW_STABLE_LOG("gwStableLog", "УУГВ"),
    ACT("act", "Акт"),
    ACT_NUMBER("actNumber", "№ акта"),
    THERMAL_TUBE("thermalTube", "Т-трубка"),
    ADDITIONAL_INFO("additionalInfo", "Комментарий"),

        // Материалы (чекбоксы)
    HAS_VIDEO("HasVideo", "Видео"),
    HAS_DRILLING("HasDrilling", "Буровая"),
    HAS_JOURNAL("HasJournal", "Журнал"),
    HAS_CORE("HasCore", "Керн"),
    HAS_STAKE("HasStake", "Штага"),
    
    // Образцы
    SAMPLES_THAWED("samplesThawed", "Талые (шт)"),
    SAMPLES_FROZEN("samplesFrozen", "Мерзлые (шт)"),
    SAMPLES_ROCKY("samplesRocky", "Скальные (шт)"),

    CAT1_4("cat1_4", "1-4, п.м."),
    CAT5_8("cat5_8", "5-8, п.м."),
    CAT9_12("cat9_12", "9-12, п.м."),
}

// Контейнер для строки, требующей исправления
class CorrectionRow(val originalRowIndex: Int, var errorMsg: String, val rawValues: MutableMap<DbField, String>)