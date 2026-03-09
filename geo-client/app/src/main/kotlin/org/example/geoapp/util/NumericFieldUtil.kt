package org.example.geoapp.util

import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import java.util.function.UnaryOperator

object NumericFieldUtil {

    private val decimalRegex = Regex("^\\d+([.,]\\d*)?$")

    fun applyDecimalFilter(field: TextField) {

        val filter = UnaryOperator<TextFormatter.Change> { change ->

            val newText = change.controlNewText

            if (newText.isEmpty()) {
                return@UnaryOperator change
            }

            if (decimalRegex.matches(newText)) {
                change
            } else {
                null
            }
        }

        field.textFormatter = TextFormatter<String>(filter)
    }

}