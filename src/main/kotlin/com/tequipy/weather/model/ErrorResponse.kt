package com.tequipy.weather.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Error response for failed requests")
data class ErrorResponse(
    @Schema(description = "Error code", example = "UPSTREAM_TIMEOUT")
    val error: String,

    @Schema(description = "Human-readable error message", example = "Failed to fetch weather data within timeout")
    val message: String,

    @Schema(description = "Timestamp of the error", example = "2026-01-11T10:12:54Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val timestamp: Instant = Instant.now(),

    @Schema(description = "Request path that caused the error", example = "/api/v1/weather/current")
    val path: String
)
