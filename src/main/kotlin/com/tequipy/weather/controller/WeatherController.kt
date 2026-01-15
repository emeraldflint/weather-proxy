package com.tequipy.weather.controller

import com.tequipy.weather.model.ErrorResponse
import com.tequipy.weather.model.WeatherResponse
import com.tequipy.weather.service.WeatherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/weather")
@Validated
@Tag(name = "Weather")
class WeatherController(
    private val weatherService: WeatherService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/current", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Get current weather for coordinates")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = WeatherResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid coordinates or missing parameters", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "502", description = "Upstream service error or invalid response", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "503", description = "Upstream service unavailable", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "504", description = "Upstream timeout", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    fun getCurrentWeather(
        @RequestParam
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        lat: Double,

        @RequestParam
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        lon: Double
    ): ResponseEntity<WeatherResponse> {
        logger.info("Received request for weather at lat={}, lon={}", lat, lon)

        val weather = weatherService.getCurrentWeather(lat, lon)

        logger.debug("Returning weather response for lat={}, lon={}", lat, lon)
        return ResponseEntity.ok(weather)
    }
}
