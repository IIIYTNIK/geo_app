package com.example.geoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import com.example.geoserver.report.engine.ReportEngine
import com.example.geoserver.report.model.*

@SpringBootApplication
class GeoServerApplication

fun main(args: Array<String>) {
	runApplication<GeoServerApplication>(*args)
}
