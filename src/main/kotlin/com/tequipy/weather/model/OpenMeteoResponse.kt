package com.tequipy.weather.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,

    @JsonProperty("generationtime_ms")
    val generationTimeMs: Double,

    @JsonProperty("utc_offset_seconds")
    val utcOffsetSeconds: Int,

    val timezone: String,

    @JsonProperty("timezone_abbreviation")
    val timezoneAbbreviation: String,

    val elevation: Double,

    @JsonProperty("current_units")
    val currentUnits: CurrentUnits?,

    val current: CurrentData?
)

data class CurrentUnits(
    val time: String,
    val interval: String,

    @JsonProperty("temperature_2m")
    val temperature2m: String,

    @JsonProperty("wind_speed_10m")
    val windSpeed10m: String
)

data class CurrentData(
    val time: String,
    val interval: Int,

    @JsonProperty("temperature_2m")
    val temperature2m: Double,

    @JsonProperty("wind_speed_10m")
    val windSpeed10m: Double
)
