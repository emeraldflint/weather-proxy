package com.tequipy.weather.service

import com.tequipy.weather.config.CacheConfig
import com.tequipy.weather.config.WeatherProperties
import com.tequipy.weather.exception.WeatherDataParsingException
import com.tequipy.weather.model.CurrentWeather
import com.tequipy.weather.model.Location
import com.tequipy.weather.model.WeatherResponse
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.Instant

@Service
class WeatherService(
    private val openMeteoClient: OpenMeteoClient,
    private val weatherProperties: WeatherProperties,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Cacheable(
        cacheNames = [CacheConfig.WEATHER_CACHE],
        key = "#root.target.createCacheKey(#latitude, #longitude)"
    )
    fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse {
        logger.info("Cache miss - fetching weather for lat={}, lon={}", latitude, longitude)
        meterRegistry.counter("weather.cache.miss").increment()

        val roundedLat = roundCoordinate(latitude)
        val roundedLon = roundCoordinate(longitude)

        val openMeteoResponse = openMeteoClient.fetchCurrentWeather(roundedLat, roundedLon)

        val currentData = openMeteoResponse.current
            ?: throw WeatherDataParsingException("No current weather data in response")

        return WeatherResponse(
            location = Location(
                lat = roundedLat,
                lon = roundedLon
            ),
            current = CurrentWeather(
                temperatureC = currentData.temperature2m,
                windSpeedKmh = currentData.windSpeed10m
            ),
            source = "open-meteo",
            retrievedAt = Instant.now()
        )
    }

    fun createCacheKey(latitude: Double, longitude: Double): String {
        val roundedLat = roundCoordinate(latitude)
        val roundedLon = roundCoordinate(longitude)
        return "$roundedLat:$roundedLon"
    }

    private fun roundCoordinate(value: Double): Double {
        val precision = weatherProperties.cache.coordinatePrecision
        return value.toBigDecimal()
            .setScale(precision, RoundingMode.HALF_UP)
            .toDouble()
    }
}
