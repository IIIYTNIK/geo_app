package org.example.geoapp.util

object FilterParser {

    fun parse(filter: String, value: Any?): Boolean {

        val trimmedFilter = filter.trim()
        val strValue = value?.toString()?.trim()

        // ===== Пустые / непустые =====

        if (trimmedFilter == "=") {
            return strValue.isNullOrEmpty()
        }

        if (trimmedFilter == "<>") {
            return !strValue.isNullOrEmpty()
        }

        if (strValue.isNullOrEmpty()) return false

        // ===== Числовые операторы =====

        if (trimmedFilter.startsWith(">=")) {
            return compareNumeric(strValue, trimmedFilter.substring(2)) { a, b -> a >= b }
        }

        if (trimmedFilter.startsWith(">")) {
            return compareNumeric(strValue, trimmedFilter.substring(1)) { a, b -> a > b }
        }

        if (trimmedFilter.startsWith("<>")) {
            val term = trimmedFilter.substring(2)
            return strValue != term
        }

        if (trimmedFilter.startsWith("=")) {
            val term = trimmedFilter.substring(1)
            return strValue == term
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
        // Если строка содержит "-" — считаем точным совпадением
        if (trimmedFilter.contains("-")) {
            return strValue.equals(trimmedFilter, ignoreCase = true)
        }

        // Если нет спецсимволов — считаем startsWith
        return strValue.startsWith(trimmedFilter, ignoreCase = true)
    }

    private fun compareNumeric(
        value: String,
        filterValue: String,
        comparator: (Double, Double) -> Boolean
    ): Boolean {

        val valNum = value.toDoubleOrNull() ?: return false
        val filterNum = filterValue.toDoubleOrNull() ?: return false

        return comparator(valNum, filterNum)
    }
}