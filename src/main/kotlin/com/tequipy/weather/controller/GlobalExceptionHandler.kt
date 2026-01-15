package com.tequipy.weather.controller

import com.tequipy.weather.exception.*
import com.tequipy.weather.model.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.net.SocketTimeoutException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UpstreamTimeoutException::class)
    fun handleUpstreamTimeout(
        ex: UpstreamTimeoutException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream timeout: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.GATEWAY_TIMEOUT)
            .body(
                ErrorResponse(
                    error = UpstreamTimeoutException.ERROR_CODE,
                    message = ex.message,
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(UpstreamServiceException::class)
    fun handleUpstreamServiceError(
        ex: UpstreamServiceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream service error: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    error = UpstreamServiceException.ERROR_CODE,
                    message = ex.message,
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(UpstreamUnavailableException::class)
    fun handleUpstreamUnavailable(
        ex: UpstreamUnavailableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream unavailable: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ErrorResponse(
                    error = UpstreamUnavailableException.ERROR_CODE,
                    message = ex.message,
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(InvalidCoordinatesException::class)
    fun handleInvalidCoordinates(
        ex: InvalidCoordinatesException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid coordinates: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "INVALID_COORDINATES",
                    message = ex.message,
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(WeatherDataParsingException::class)
    fun handleParsingError(
        ex: WeatherDataParsingException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Parsing error: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    error = WeatherDataParsingException.ERROR_CODE,
                    message = ex.message,
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Missing parameter: ${ex.parameterName}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "MISSING_PARAMETER",
                    message = "Required parameter '${ex.parameterName}' is missing",
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleValidationException(
        ex: HandlerMethodValidationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.allErrors
            .mapNotNull { it.defaultMessage }
            .joinToString("; ")
        logger.warn("Validation failed: $errors")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "VALIDATION_ERROR",
                    message = errors.ifEmpty { "Validation failed" },
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations
            .joinToString("; ") { it.message }
        logger.warn("Constraint violation: $errors")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "VALIDATION_ERROR",
                    message = errors.ifEmpty { "Validation failed" },
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientError(
        ex: HttpClientErrorException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream client error: {}", ex.statusCode)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    error = "UPSTREAM_ERROR",
                    message = "Upstream service returned ${ex.statusCode}",
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(HttpServerErrorException::class)
    fun handleHttpServerError(
        ex: HttpServerErrorException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream server error: {}", ex.statusCode)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    error = "UPSTREAM_ERROR",
                    message = "Upstream service error",
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(ResourceAccessException::class)
    fun handleResourceAccess(
        ex: ResourceAccessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Upstream connection error: {}", ex.message)
        val isTimeout = ex.mostSpecificCause is SocketTimeoutException
        return if (isTimeout) {
            ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(
                    ErrorResponse(
                        error = "UPSTREAM_TIMEOUT",
                        message = "Upstream service timeout",
                        timestamp = Instant.now(),
                        path = request.requestURI
                    )
                )
        } else {
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    ErrorResponse(
                        error = "UPSTREAM_UNAVAILABLE",
                        message = "Cannot connect to upstream service",
                        timestamp = Instant.now(),
                        path = request.requestURI
                    )
                )
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    timestamp = Instant.now(),
                    path = request.requestURI
                )
            )
    }
}
