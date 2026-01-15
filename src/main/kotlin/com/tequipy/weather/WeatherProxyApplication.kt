package com.tequipy.weather

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.resilience.annotation.EnableResilientMethods

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableResilientMethods
class WeatherProxyApplication

fun main(args: Array<String>) {
    runApplication<WeatherProxyApplication>(*args)
}
