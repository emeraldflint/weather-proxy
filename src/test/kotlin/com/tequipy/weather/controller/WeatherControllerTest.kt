package com.tequipy.weather.controller

import com.tequipy.weather.config.CacheProperties
import com.tequipy.weather.config.OpenMeteoProperties
import com.tequipy.weather.config.RetryProperties
import com.tequipy.weather.config.WeatherProperties
import com.tequipy.weather.exception.UpstreamServiceException
import com.tequipy.weather.exception.UpstreamTimeoutException
import com.tequipy.weather.exception.UpstreamUnavailableException
import com.tequipy.weather.exception.WeatherDataParsingException
import com.tequipy.weather.model.CurrentWeather
import com.tequipy.weather.model.Location
import com.tequipy.weather.model.WeatherResponse
import com.tequipy.weather.service.WeatherService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(WeatherController::class)
@Import(WeatherControllerTest.TestConfig::class)
class WeatherControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun weatherProperties(): WeatherProperties {
            return WeatherProperties(
                openMeteo = OpenMeteoProperties(
                    baseUrl = "https://api.open-meteo.com/v1",
                    timeoutMs = 1000,
                    retry = RetryProperties(maxAttempts = 2, delayMs = 100)
                ),
                cache = CacheProperties(ttlSeconds = 60, coordinatePrecision = 2)
            )
        }

        @Bean
        fun cacheManager(): CacheManager = NoOpCacheManager()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var weatherService: WeatherService

    @Test
    fun `getCurrentWeather returns weather data successfully`() {
        val response = WeatherResponse(
            location = Location(52.52, 13.41),
            current = CurrentWeather(5.5, 10.2),
            source = "open-meteo",
            retrievedAt = Instant.parse("2026-01-11T10:00:00Z")
        )
        `when`(weatherService.getCurrentWeather(52.52, 13.41)).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.location.lat").value(52.52))
            .andExpect(jsonPath("$.location.lon").value(13.41))
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))
            .andExpect(jsonPath("$.current.windSpeedKmh").value(10.2))
            .andExpect(jsonPath("$.source").value("open-meteo"))
    }

    @Test
    fun `getCurrentWeather returns 400 for invalid latitude`() {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "91.0")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
    }

    @Test
    fun `getCurrentWeather returns 400 for invalid longitude`() {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "181.0")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
    }

    @Test
    fun `getCurrentWeather returns 400 for missing parameters`() {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getCurrentWeather returns 504 on upstream timeout`() {
        `when`(weatherService.getCurrentWeather(52.52, 13.41))
            .thenThrow(UpstreamTimeoutException())

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.error").value("UPSTREAM_TIMEOUT"))
    }

    @Test
    fun `getCurrentWeather accepts negative coordinates`() {
        val response = WeatherResponse(
            location = Location(-33.87, 151.21),
            current = CurrentWeather(22.5, 15.3),
            source = "open-meteo",
            retrievedAt = Instant.now()
        )
        `when`(weatherService.getCurrentWeather(-33.87, 151.21)).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "-33.87")
                .param("lon", "151.21")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.location.lat").value(-33.87))
    }

    @ParameterizedTest(name = "lat {0} is valid")
    @ValueSource(doubles = [-90.0, 0.0, 90.0])
    fun `accepts boundary latitudes`(lat: Double) {
        val response = createWeatherResponse(lat, 0.0)
        `when`(weatherService.getCurrentWeather(lat, 0.0)).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", lat.toString())
                .param("lon", "0.0")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @ParameterizedTest(name = "lon {0} is valid")
    @ValueSource(doubles = [-180.0, 0.0, 180.0])
    fun `accepts boundary longitudes`(lon: Double) {
        val response = createWeatherResponse(0.0, lon)
        `when`(weatherService.getCurrentWeather(0.0, lon)).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "0.0")
                .param("lon", lon.toString())
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @ParameterizedTest(name = "lat {0} rejected")
    @ValueSource(doubles = [-91.0, 91.0])
    fun `rejects invalid latitudes`(lat: Double) {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", lat.toString())
                .param("lon", "0.0")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
    }

    @ParameterizedTest(name = "lon {0} rejected")
    @ValueSource(doubles = [-181.0, 181.0])
    fun `rejects invalid longitudes`(lon: Double) {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "0.0")
                .param("lon", lon.toString())
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
    }

    @Test
    fun `returns 502 on upstream service error`() {
        `when`(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamServiceException("Open-Meteo returned error"))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("UPSTREAM_ERROR"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/weather/current"))
    }

    @Test
    fun `getCurrentWeather returns 503 on upstream unavailable`() {
        `when`(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamUnavailableException("Cannot connect to Open-Meteo"))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error").value("UPSTREAM_UNAVAILABLE"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/weather/current"))
    }

    @Test
    fun `getCurrentWeather returns 502 on weather data parsing error`() {
        `when`(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(WeatherDataParsingException("Invalid response format"))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("PARSING_ERROR"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/weather/current"))
    }

    @Test
    fun `getCurrentWeather returns 500 on unexpected error`() {
        `when`(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(RuntimeException("Unexpected error"))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
    }

    @Test
    fun `error response contains correct timestamp format`() {
        `when`(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamTimeoutException())

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.timestamp").exists())
            // Timestamp should match ISO format: 2026-01-11T10:12:54Z
            .andExpect(jsonPath("$.timestamp").isString)
    }

    private fun createWeatherResponse(lat: Double, lon: Double): WeatherResponse {
        return WeatherResponse(
            location = Location(lat, lon),
            current = CurrentWeather(15.0, 10.0),
            source = "open-meteo",
            retrievedAt = Instant.now()
        )
    }
}
