package org.example.geoapp.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object FilterParser {

    // Поддерживаемые форматы дат (БД обычно отдает yyyy-MM-dd, а пользователь может вводить dd.MM.yyyy)
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yy")
    )

    fun parse(filter: String, value: Any?): Boolean {
        val trimmedFilter = filter.trim()
        val strValue = value?.toString()?.trim()

        // ===== Пустые / непустые =====
        if (trimmedFilter == "=") return strValue.isNullOrEmpty()
        if (trimmedFilter == "<>") return !strValue.isNullOrEmpty()
        if (strValue.isNullOrEmpty()) return false

        // ===== Операторы сравнения =====
        if (trimmedFilter.startsWith(">=")) {
            return compare(strValue, trimmedFilter.substring(2).trim()) { it >= 0 }
        }
        if (trimmedFilter.startsWith("<=")) {
            return compare(strValue, trimmedFilter.substring(2).trim()) { it <= 0 }
        }
        if (trimmedFilter.startsWith(">")) {
            return compare(strValue, trimmedFilter.substring(1).trim()) { it > 0 }
        }
        if (trimmedFilter.startsWith("<>")) {
            return strValue != trimmedFilter.substring(2).trim()
        }
        if (trimmedFilter.startsWith("<")) {
            return compare(strValue, trimmedFilter.substring(1).trim()) { it < 0 }
        }
        if (trimmedFilter.startsWith("=")) {
            return strValue == trimmedFilter.substring(1).trim()
        }

        // ===== С маской в начале =====
        if (trimmedFilter.startsWith("*")) {
            val term = trimmedFilter.substring(1)
            return strValue.contains(term, ignoreCase = true)
        }

        // ===== С маской в конце =====
        if (trimmedFilter.endsWith("*")) {
            val term = trimmedFilter.dropLast(1)
            return strValue.startsWith(term, ignoreCase = true)
        }

        // ===== Без спецсимволов =====
        // Если строка содержит "-" или "." (для дат) — считаем точным совпадением
        if (trimmedFilter.contains("-") || trimmedFilter.contains(".")) {
            // Если это дата, попробуем сравнить "по-умному" (например 01.05.2026 и 2026-05-01)
            val valDate = parseDate(strValue)
            val filterDate = parseDate(trimmedFilter)
            if (valDate != null && filterDate != null) {
                return valDate == filterDate
            }
            return strValue.equals(trimmedFilter, ignoreCase = true)
        }

        // Если нет спецсимволов — считаем startsWith
        return strValue.startsWith(trimmedFilter, ignoreCase = true)
    }

    /**
     * Функция сравнения.
     * Пытается сравнить значения как числа, а затем как даты.
     * Возвращает результат comparator, куда передается результат compareTo (-1, 0, 1)
     */
    private fun compare(valueStr: String, filterStr: String, comparator: (Int) -> Boolean): Boolean {
        // Пробуем парсить как числа (заменяем запятую на точку для надежности)
        val valNum = valueStr.replace(",", ".").toDoubleOrNull()
        val filterNum = filterStr.replace(",", ".").toDoubleOrNull()

        if (valNum != null && filterNum != null) {
            return comparator(valNum.compareTo(filterNum))
        }

        // Пробуем парсить как даты
        val valDate = parseDate(valueStr)
        val filterDate = parseDate(filterStr)

        if (valDate != null && filterDate != null) {
            return comparator(valDate.compareTo(filterDate))
        }

        return false
    }

    /**
     * Попытка распарсить строку в LocalDate по списку форматов
     */
    private fun parseDate(str: String): LocalDate? {
        for (formatter in dateFormats) {
            try {
                return LocalDate.parse(str, formatter)
            } catch (e: DateTimeParseException) {
                // Игнорируем ошибку и пробуем следующий формат
                continue
            }
        }
        return null
    }
}