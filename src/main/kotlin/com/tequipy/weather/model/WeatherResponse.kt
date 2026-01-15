package com.tequipy.weather.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class WeatherResponse(
    val location: Location,
    val current: CurrentWeather,
    val source: String = "open-meteo",
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val retrievedAt: Instant
)

data class Location(
    val lat: Double,
    val lon: Double
)

data class CurrentWeather(
    val temperatureC: Double,
    val windSpeedKmh: Double
)
