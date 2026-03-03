package org.example.geoapp.util

import javafx.application.Platform
import javafx.collections.transformation.FilteredList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ScrollBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import java.util.function.Predicate

class AutoFilterRow<T>(
    private val tableView: TableView<T>,
    private val filteredList: FilteredList<T>,
    private val onFilterChanged: () -> Unit
) {

    val node: HBox = HBox().apply {
        spacing = 0.0
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: white;"
    }

    private val columnFieldMap = mutableMapOf<TableColumn<T, *>, TextField>()

    init {
        createFilterFields()

        // Ждём, пока таблица построится
        node.prefWidthProperty().bind(tableView.widthProperty())
    }

    private fun createFilterFields() {
        node.children.clear()
        columnFieldMap.clear()

        tableView.columns.forEach { column ->

            val filterField = TextField().apply {
                prefHeight = 26.0
                promptText = ""

                style = """
                    -fx-background-color: white;
                    -fx-border-color: #BDBDBD;
                    -fx-border-width: 0 1 1 0;
                    -fx-padding: 0 4 0 4;
                """.trimIndent()

                prefWidthProperty().bind(column.widthProperty())

                textProperty().addListener { _, _, _ ->
                    applyFilters()
                }
            }

            columnFieldMap[column] = filterField
            node.children.add(filterField
            )
        }
    }


    private fun findHorizontalScrollBar(node: Node): ScrollBar? {
        if (node is ScrollBar && node.orientation == Orientation.HORIZONTAL) {
            return node
        }
        if (node is javafx.scene.Parent) {
            node.childrenUnmodifiable.forEach {
                val result = findHorizontalScrollBar(it)
                if (result != null) return result
            }
        }
        return null
    }

    fun applyFilters() {

        filteredList.predicate = Predicate { row ->

            columnFieldMap.all { (column, field) ->

                val filterText = field.text.trim()
                if (filterText.isEmpty()) return@all true

                val cellValue = try {
                    column.getCellData(row)
                } catch (e: Exception) {
                    null
                }

                FilterParser.parse(filterText, cellValue)
            }
        }

        onFilterChanged()
    }
}