package com.tequipy.weather.integration

import com.tequipy.weather.config.CacheConfig
import com.tequipy.weather.config.TestRestClientConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.hamcrest.Matchers.containsString

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRestClientConfig::class)
class CacheBehaviorTest {

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
    fun `second request for same coordinates returns cached data`() {
        val responseBody = createWeatherResponse(52.52, 13.41, 5.5, 10.2)

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))

        mockServer.verify()
    }

    @Test
    fun `coordinates within same precision share cache entry`() {
        val responseBody = createWeatherResponse(52.52, 13.41, 5.5, 10.2)

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        // 52.5212 rounds to 52.52
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.5212")
                .param("lon", "13.4134")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // 52.5248 also rounds to 52.52 - should hit cache
        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.5248")
                .param("lon", "13.4149")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        mockServer.verify()
    }

    @Test
    fun `different coordinates use separate cache entries`() {
        val berlinResponse = createWeatherResponse(52.52, 13.41, 5.5, 10.2)
        val parisResponse = createWeatherResponse(48.85, 2.35, 8.0, 15.0)

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("latitude=52.52")))
            .andRespond(withSuccess(berlinResponse, MediaType.APPLICATION_JSON))

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("latitude=48.85")))
            .andRespond(withSuccess(parisResponse, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "48.85")
                .param("lon", "2.35")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(8.0))

        mockServer.verify()
    }

    @Test
    fun `cache clear causes fresh fetch`() {
        val response1 = createWeatherResponse(52.52, 13.41, 5.5, 10.2)
        val response2 = createWeatherResponse(52.52, 13.41, 6.0, 12.0)

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withSuccess(response1, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(5.5))

        mockServer.verify()

        cacheManager.getCache(CacheConfig.WEATHER_CACHE)?.clear()
        mockServer.reset()

        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/forecast")))
            .andRespond(withSuccess(response2, MediaType.APPLICATION_JSON))

        mockMvc.perform(
            get("/api/v1/weather/current")
                .param("lat", "52.52")
                .param("lon", "13.41")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.temperatureC").value(6.0))

        mockServer.verify()
    }

    private fun createWeatherResponse(lat: Double, lon: Double, temp: Double, wind: Double): String = """
        {
            "latitude": $lat,
            "longitude": $lon,
            "generationtime_ms": 0.5,
            "utc_offset_seconds": 0,
            "timezone": "UTC",
            "timezone_abbreviation": "UTC",
            "elevation": 38.0,
            "current": {
                "time": "2026-01-11T10:00",
                "interval": 900,
                "temperature_2m": $temp,
                "wind_speed_10m": $wind
            }
        }
    """.trimIndent()
}
