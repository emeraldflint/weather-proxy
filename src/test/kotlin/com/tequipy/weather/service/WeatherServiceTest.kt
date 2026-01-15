package com.tequipy.weather.service

import com.tequipy.weather.config.CacheProperties
import com.tequipy.weather.config.OpenMeteoProperties
import com.tequipy.weather.config.RetryProperties
import com.tequipy.weather.config.WeatherProperties
import com.tequipy.weather.exception.UpstreamServiceException
import com.tequipy.weather.exception.UpstreamTimeoutException
import com.tequipy.weather.exception.UpstreamUnavailableException
import com.tequipy.weather.exception.WeatherDataParsingException
import com.tequipy.weather.model.CurrentData
import com.tequipy.weather.model.OpenMeteoResponse
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@ExtendWith(MockitoExtension::class)
class WeatherServiceTest {

    @Mock
    private lateinit var openMeteoClient: OpenMeteoClient

    private lateinit var weatherService: WeatherService
    private lateinit var meterRegistry: SimpleMeterRegistry

    private val weatherProperties = WeatherProperties(
        openMeteo = OpenMeteoProperties(
            baseUrl = "https://api.open-meteo.com/v1",
            timeoutMs = 1000,
            retry = RetryProperties(maxAttempts = 2, delayMs = 100)
        ),
        cache = CacheProperties(ttlSeconds = 60, coordinatePrecision = 2)
    )

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        weatherService = WeatherService(openMeteoClient, weatherProperties, meterRegistry)
    }

    @Test
    fun `getCurrentWeather returns mapped response`() {
        val openMeteoResponse = createOpenMeteoResponse(52.52, 13.41, 5.5, 10.2)
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble())).thenReturn(openMeteoResponse)

        val result = weatherService.getCurrentWeather(52.52, 13.41)

        assertThat(result.location.lat).isEqualTo(52.52)
        assertThat(result.location.lon).isEqualTo(13.41)
        assertThat(result.current.temperatureC).isEqualTo(5.5)
        assertThat(result.current.windSpeedKmh).isEqualTo(10.2)
        assertThat(result.source).isEqualTo("open-meteo")
    }

    @Test
    fun `rounds coordinates to 2 decimals`() {
        val openMeteoResponse = createOpenMeteoResponse(52.52, 13.41, 5.5, 10.2)
        `when`(openMeteoClient.fetchCurrentWeather(52.52, 13.41)).thenReturn(openMeteoResponse)

        weatherService.getCurrentWeather(52.5234567, 13.4123456)

        verify(openMeteoClient).fetchCurrentWeather(52.52, 13.41)
    }

    @Test
    fun `createCacheKey generates correct key format`() {
        val key = weatherService.createCacheKey(52.5234567, 13.4123456)

        assertThat(key).isEqualTo("52.52:13.41")
    }

    @Test
    fun `createCacheKey rounds negative coordinates correctly`() {
        val key = weatherService.createCacheKey(-33.8567, 151.2093)

        assertThat(key).isEqualTo("-33.86:151.21")
    }

    @Test
    fun `increments cache miss counter`() {
        val openMeteoResponse = createOpenMeteoResponse(52.52, 13.41, 5.5, 10.2)
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble())).thenReturn(openMeteoResponse)

        weatherService.getCurrentWeather(52.52, 13.41)

        val counter = meterRegistry.counter("weather.cache.miss")
        assertThat(counter.count()).isEqualTo(1.0)
    }

    @Test
    fun `throws WeatherDataParsingException when current data is null`() {
        val responseWithNullCurrent = OpenMeteoResponse(
            latitude = 52.52,
            longitude = 13.41,
            generationTimeMs = 0.5,
            utcOffsetSeconds = 0,
            timezone = "UTC",
            timezoneAbbreviation = "UTC",
            elevation = 38.0,
            currentUnits = null,
            current = null
        )
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble())).thenReturn(responseWithNullCurrent)

        val exception = assertThrows<WeatherDataParsingException> {
            weatherService.getCurrentWeather(52.52, 13.41)
        }

        assertThat(exception.message).isEqualTo("No current weather data in response")
    }

    @Test
    fun `propagates UpstreamTimeoutException`() {
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamTimeoutException("Timeout occurred"))

        assertThrows<UpstreamTimeoutException> {
            weatherService.getCurrentWeather(52.52, 13.41)
        }
    }

    @Test
    fun `propagates UpstreamServiceException`() {
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamServiceException("Service error"))

        assertThrows<UpstreamServiceException> {
            weatherService.getCurrentWeather(52.52, 13.41)
        }
    }

    @Test
    fun `propagates UpstreamUnavailableException`() {
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble()))
            .thenThrow(UpstreamUnavailableException("Service unavailable"))

        assertThrows<UpstreamUnavailableException> {
            weatherService.getCurrentWeather(52.52, 13.41)
        }
    }

    @Test
    fun `returns response with all required fields`() {
        val openMeteoResponse = createOpenMeteoResponse(52.52, 13.41, 5.5, 10.2)
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble())).thenReturn(openMeteoResponse)

        val result = weatherService.getCurrentWeather(52.52, 13.41)

        assertThat(result.location).isNotNull()
        assertThat(result.current).isNotNull()
        assertThat(result.source).isNotNull()
        assertThat(result.retrievedAt).isNotNull()
    }

    @Test
    fun `getCurrentWeather sets retrievedAt to current time`() {
        val openMeteoResponse = createOpenMeteoResponse(52.52, 13.41, 5.5, 10.2)
        `when`(openMeteoClient.fetchCurrentWeather(anyDouble(), anyDouble())).thenReturn(openMeteoResponse)

        val before = java.time.Instant.now()
        val result = weatherService.getCurrentWeather(52.52, 13.41)
        val after = java.time.Instant.now()

        assertThat(result.retrievedAt).isAfterOrEqualTo(before)
        assertThat(result.retrievedAt).isBeforeOrEqualTo(after)
    }

    private fun createOpenMeteoResponse(
        lat: Double,
        lon: Double,
        temp: Double,
        wind: Double
    ): OpenMeteoResponse {
        return OpenMeteoResponse(
            latitude = lat,
            longitude = lon,
            generationTimeMs = 0.5,
            utcOffsetSeconds = 0,
            timezone = "UTC",
            timezoneAbbreviation = "UTC",
            elevation = 38.0,
            currentUnits = null,
            current = CurrentData(
                time = "2026-01-11T10:00",
                interval = 900,
                temperature2m = temp,
                windSpeed10m = wind
            )
        )
    }
}
