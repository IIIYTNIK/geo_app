package org.example.geoapp.util

fun String.toBearerAuthorization(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    return if (trimmed.startsWith("Bearer ", ignoreCase = true)) trimmed else "Bearer $trimmed"
}
