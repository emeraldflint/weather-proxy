package com.tequipy.weather.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "weather")
data class WeatherProperties(
    val openMeteo: OpenMeteoProperties,
    val cache: CacheProperties
)

data class OpenMeteoProperties(
    val baseUrl: String,
    val timeoutMs: Long,
    val retry: RetryProperties
)

data class RetryProperties(
    val maxAttempts: Int,
    val delayMs: Long
)

data class CacheProperties(
    val ttlSeconds: Long,
    val coordinatePrecision: Int
)
