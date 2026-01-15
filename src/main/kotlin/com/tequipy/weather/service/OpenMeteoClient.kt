package com.tequipy.weather.service

import com.tequipy.weather.exception.WeatherDataParsingException
import com.tequipy.weather.model.OpenMeteoResponse
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class OpenMeteoClient(
    private val openMeteoRestClient: RestClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Timed(value = "openmeteo.request", description = "Time taken to fetch weather from Open-Meteo")
    @Retryable(
        includes = [ResourceAccessException::class, HttpServerErrorException::class],
        maxRetries = 2,
        delay = 100
    )
    fun fetchCurrentWeather(latitude: Double, longitude: Double): OpenMeteoResponse {
        logger.debug("Fetching weather for lat={}, lon={}", latitude, longitude)

        return openMeteoRestClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/forecast")
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current", "temperature_2m,wind_speed_10m")
                    .build()
            }
            .retrieve()
            .body<OpenMeteoResponse>()
            ?: throw WeatherDataParsingException("Received null response from Open-Meteo")
    }
}
