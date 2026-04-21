package com.example.geoserver.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

@Service
public class JavaReportService {

    private final DataSource dataSource;

    public JavaReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public byte[] generateDrillingCompletedReportPdf(
            LocalDate reportStart,
            LocalDate reportEnd,
            Long contractorId,
            Long areaId
    ) {
        try (Connection connection = dataSource.getConnection()) {
            // Загружаем JRXML шаблон (если есть) или создаем отчет программно
            InputStream reportStream = new ClassPathResource("reports/drilling_report.jrxml").getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Параметры отчета
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_START", reportStart);
            parameters.put("REPORT_END", reportEnd);
            parameters.put("CONTRACTOR_ID", contractorId);
            parameters.put("AREA_ID", areaId);

            // Создаем источник данных из SQL запроса
            JRDataSource dataSource = new JREmptyDataSource();
            // Или используем JDBC: new JRResultSetDataSource(connection.createStatement().executeQuery(sql))

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);

            // Экспорт в PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            exporter.exportReport();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }
}