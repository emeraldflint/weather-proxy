package com.tequipy.weather.integration

import com.tequipy.weather.config.CacheConfig
import com.tequipy.weather.config.TestRestClientConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.containsString

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRestClientConfig::class)
class WeatherIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var mockServer: MockRestServiceServer

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        cacheManager.getCache(CacheConfig.WEATHER_CACHE)?.clear()
        mockServer.reset()
    }

    @Test
    fun `full request flow returns correct response`() {
        val openMeteoResponse = """
            {
                "latitude": 52.52,
                "longitude": 13.41,
                "generationtime_ms": 0.5,
                "utc_offset_seconds": 0,
                "timezone": "UTC",
                "timezone_abbreviation": "UTC",
                "elevation": 38.0,
                "current_units": {
                    "time": "iso8601",
                    "interval": "seconds",
                    "temperature_2m": "°C",
                    "wind_speed_10m": "km/h"
                },
                "current": {
                    "time": "2026-01-11T10:00",
                    "interval": 900,
                    "temperature_2m": 5.5,
                    "wind_speed_10m": 10.2
                }
            }
        """.trimIndent()

        mockServer.expect(requestTo(containsString("/forecast")))
            .andRespond(withSuccess(openMeteoResponse, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.location.lat").value(52.52))
            .andExpect(jsonPath("$.location.lon").value(13.41))
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))
            .andExpect(jsonPath("$.current.windSpeedKmh").value(10.2))
            .andExpect(jsonPath("$.source").value("open-meteo"))
            .andExpect(jsonPath("$.retrievedAt").exists())

        mockServer.verify()
    }

    @Test
    fun `retries on server error and succeeds`() {
        val successResponse = """
            {
                "latitude": 52.53,
                "longitude": 13.42,
                "generationtime_ms": 0.5,
                "utc_offset_seconds": 0,
                "timezone": "UTC",
                "timezone_abbreviation": "UTC",
                "elevation": 38.0,
                "current": {
                    "time": "2026-01-11T10:00",
                    "interval": 900,
                    "temperature_2m": 5.5,
                    "wind_speed_10m": 10.2
                }
            }
        """.trimIndent()

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withServiceUnavailable())

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withSuccess(successResponse, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.53")
                .param("lon", "13.42")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))

        mockServer.verify()
    }

    @Test
    fun `client error returns 502 without retry`() {
        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withBadRequest())

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.54")
                .param("lon", "13.43")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("UPSTREAM_ERROR"))

        mockServer.verify()
    }

    @Test
    fun `health endpoints work`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))

        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk)
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk)
    }

    @Test
    fun `prometheus metrics are exposed`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("http_server_requests")))
    }

    @Test
    fun `swagger ui works`() {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths./api/v1/weather/current").exists())
    }

    @Test
    fun `response does not leak upstream fields`() {
        val openMeteoResponse = """
            {
                "latitude": 52.52,
                "longitude": 13.41,
                "generationtime_ms": 0.5,
                "utc_offset_seconds": 0,
                "timezone": "UTC",
                "timezone_abbreviation": "UTC",
                "elevation": 38.0,
                "current": {
                    "time": "2026-01-11T10:00",
                    "interval": 900,
                    "temperature_2m": 5.5,
                    "wind_speed_10m": 10.2
                }
            }
        """.trimIndent()

        mockServer.expect(requestTo(containsString("/forecast")))
            .andRespond(withSuccess(openMeteoResponse, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.55")
                .param("lon", "13.44")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.location").isMap)
            .andExpect(jsonPath("$.current").isMap)
            .andExpect(jsonPath("$.latitude").doesNotExist())
            .andExpect(jsonPath("$.longitude").doesNotExist())
            .andExpect(jsonPath("$.generationtime_ms").doesNotExist())

        mockServer.verify()
    }

    @Test
    fun `invalid coordinates return 400`() {
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "999")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").isString)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.path").value("/api/v1/weather/current"))
    }
}
