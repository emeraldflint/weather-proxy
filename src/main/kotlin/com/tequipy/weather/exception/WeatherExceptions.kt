package com.tequipy.weather.exception

sealed class WeatherException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class UpstreamTimeoutException(
    message: String = "Failed to fetch weather data within timeout",
    cause: Throwable? = null
) : WeatherException(message, cause) {
    companion object {
        const val ERROR_CODE = "UPSTREAM_TIMEOUT"
    }
}

class UpstreamServiceException(
    message: String = "Upstream weather service returned an error",
    cause: Throwable? = null
) : WeatherException(message, cause) {
    companion object {
        const val ERROR_CODE = "UPSTREAM_ERROR"
    }
}

class UpstreamUnavailableException(
    message: String = "Upstream weather service is unavailable",
    cause: Throwable? = null
) : WeatherException(message, cause) {
    companion object {
        const val ERROR_CODE = "UPSTREAM_UNAVAILABLE"
    }
}

class InvalidCoordinatesException(
    message: String = "Invalid coordinates provided"
) : WeatherException(message)

class WeatherDataParsingException(
    message: String = "Failed to parse weather data from upstream",
    cause: Throwable? = null
) : WeatherException(message, cause) {
    companion object {
        const val ERROR_CODE = "PARSING_ERROR"
    }
}
