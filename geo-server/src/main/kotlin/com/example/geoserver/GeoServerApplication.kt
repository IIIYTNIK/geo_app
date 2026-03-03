package com.example.geoserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GeoServerApplication

fun main(args: Array<String>) {
	runApplication<GeoServerApplication>(*args)
}
