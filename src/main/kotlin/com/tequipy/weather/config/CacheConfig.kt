package com.tequipy.weather.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class CacheConfig(private val weatherProperties: WeatherProperties) {

    companion object {
        const val WEATHER_CACHE = "weatherCache"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager(WEATHER_CACHE)
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(weatherProperties.cache.ttlSeconds))
                .recordStats()
                .maximumSize(10_000)
        )
        return cacheManager
    }
}
