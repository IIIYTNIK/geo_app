module org.example.geoapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.javafx;

    requires retrofit2;
    requires retrofit2.converter.gson;
    requires okhttp3;
    requires okio;
    requires com.google.gson;

    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.desktop;

    opens org.example.geoapp to javafx.fxml, javafx.graphics, javafx.controls;
    opens org.example.geoapp.controller to javafx.fxml;

    opens com.example.geoapp.api to com.google.gson;
    opens com.example.geoapp.api.report to com.google.gson;
}