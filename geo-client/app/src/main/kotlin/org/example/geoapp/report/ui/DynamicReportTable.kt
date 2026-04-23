package org.example.geoapp.report.ui

import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.util.Callback
import org.example.geoapp.report.model.ColumnConfig
import org.example.geoapp.report.model.ReportRow
import org.example.geoapp.report.model.ReportTable
import javafx.beans.property.SimpleObjectProperty

/**
 * Динамически строит TableView по описанию секции и колонок.
 */
object DynamicReportTable {
    fun buildTable(table: ReportTable): TableView<ReportRow> {
        val tableView = TableView<ReportRow>()
        tableView.isEditable = false
        tableView.columns.clear()
        for (colConfig in table.section.columns) {
            val column = TableColumn<ReportRow, Any?>(colConfig.header)
            column.cellValueFactory = Callback { cellData ->
                SimpleObjectProperty(cellData.value.values[colConfig.field])
            }
            colConfig.width?.let { column.prefWidth = it }
            tableView.columns.add(column)
        }
        tableView.items.setAll(table.rows)
        return tableView
    }
}
