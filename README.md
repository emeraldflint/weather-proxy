# Weather Proxy API

A production-ready REST API that proxies current weather data from [Open-Meteo](https://open-meteo.com/), built with Spring Boot 4.0.1 and Kotlin 2.3.0.

## Features

- **Weather Data Proxy**: Fetches current temperature and wind speed for any coordinates
- **Caching**: 60-second TTL cache by coordinates (rounded to 2 decimal places)
- **Retry Logic**: Automatic retry on upstream failures (5xx errors)
- **Timeout Handling**: 1-second upstream timeout with proper error responses
- **Observability**: Prometheus metrics, health probes, structured logging
- **API Documentation**: OpenAPI 3.0 with Swagger UI
- **Kubernetes Ready**: Docker image, K8s manifests, HPA

## Requirements

- Java 25+
- Gradle 9.2.1 (wrapper included)

## Quick Start

```bash
./gradlew build      # Build
./gradlew bootRun    # Run
./gradlew test       # Test
```

The API will be available at `http://localhost:8080`

## API Usage

### Get Current Weather

```bash
curl "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41"
```

**Response:**
```json
{
  "location": {
    "lat": 52.52,
    "lon": 13.41
  },
  "current": {
    "temperatureC": 5.5,
    "windSpeedKmh": 10.2
  },
  "source": "open-meteo",
  "retrievedAt": "2026-01-14T10:30:00Z"
}
```

### Parameters

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `lat` | Double | -90 to 90 | Latitude |
| `lon` | Double | -180 to 180 | Longitude |

### Responses

| Status | Description |
|--------|-------------|
| 200 | Success |
| 400 | Invalid coordinates or missing parameters |
| 502 | Open-Meteo returned an error |
| 503 | Cannot connect to Open-Meteo |
| 504 | Open-Meteo request timed out |

## Configuration

| Property | Env Variable | Default |
|----------|--------------|---------|
| `weather.open-meteo.timeout-ms` | `WEATHER_OPEN_METEO_TIMEOUT_MS` | `1000` |
| `weather.open-meteo.retry.max-attempts` | `WEATHER_OPEN_METEO_RETRY_MAX_ATTEMPTS` | `2` |
| `weather.cache.ttl-seconds` | `WEATHER_CACHE_TTL_SECONDS` | `60` |

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/weather/current` | Get current weather |
| `GET /actuator/health` | Health check |
| `GET /actuator/prometheus` | Prometheus metrics |
| `GET /swagger-ui.html` | Swagger UI |

## Docker

```bash
docker build -t weather-proxy:v1 .
docker run -p 8080:8080 weather-proxy:v1
```

## Kubernetes

```bash
kubectl apply -f k8s/
kubectl get pods -l app=weather-proxy
```

Includes Deployment, Service, and HPA.

## Tech Stack

- Spring Boot 4.0.1
- Kotlin 2.3.0
- Gradle 9.2.1
- Caffeine (cache)
- Micrometer + Prometheus (metrics)
- SpringDoc OpenAPI (docs)
