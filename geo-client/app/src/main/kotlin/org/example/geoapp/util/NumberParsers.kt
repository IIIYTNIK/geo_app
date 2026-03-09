package org.example.geoapp.util

import javafx.scene.control.TextField

object NumberParsers {

    fun TextField.toDoubleSafe(): Double? {

        val txt = this.text.trim()

        if (txt.isEmpty()) {
            return null
        }

        return txt
            .replace(",", ".")
            .toDoubleOrNull()
    }
}