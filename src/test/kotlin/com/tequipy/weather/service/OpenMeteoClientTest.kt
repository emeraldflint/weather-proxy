package com.tequipy.weather.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString

class OpenMeteoClientTest {

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var openMeteoClient: OpenMeteoClient

    @BeforeEach
    fun setUp() {
        val restClientBuilder = RestClient.builder()
            .baseUrl("https://api.open-meteo.com/v1")

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        openMeteoClient = OpenMeteoClient(restClientBuilder.build())
    }

    @Test
    fun `fetchCurrentWeather returns parsed response`() {
        mockServer.expect(requestTo(containsString("/forecast")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON))

        val result = openMeteoClient.fetchCurrentWeather(52.52, 13.41)

        assertThat(result.latitude).isEqualTo(52.52)
        assertThat(result.longitude).isEqualTo(13.41)
        assertThat(result.current?.temperature2m).isEqualTo(5.5)
        assertThat(result.current?.windSpeed10m).isEqualTo(10.2)
        mockServer.verify()
    }

    @Test
    fun `fetchCurrentWeather throws on client error`() {
        mockServer.expect(requestTo(containsString("/forecast")))
            .andRespond(withBadRequest())

        assertThrows<HttpClientErrorException> {
            openMeteoClient.fetchCurrentWeather(52.52, 13.41)
        }
        mockServer.verify()
    }

    @Test
    fun `fetchCurrentWeather sends correct query parameters`() {
        mockServer.expect(requestTo(containsString("latitude=52.52")))
            .andExpect(requestTo(containsString("longitude=13.41")))
            .andExpect(requestTo(containsString("current=temperature_2m,wind_speed_10m")))
            .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON))

        openMeteoClient.fetchCurrentWeather(52.52, 13.41)
        mockServer.verify()
    }

    private fun successResponse() = """
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
}
