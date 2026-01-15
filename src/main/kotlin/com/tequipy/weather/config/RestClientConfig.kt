package com.tequipy.weather.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig(private val weatherProperties: WeatherProperties) {

    @Bean
    fun openMeteoRestClientBuilder(): RestClient.Builder {
        val timeout = Duration.ofMillis(weatherProperties.openMeteo.timeoutMs)

        val httpClient = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build()

        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(timeout)

        return RestClient.builder()
            .baseUrl(weatherProperties.openMeteo.baseUrl)
            .requestFactory(requestFactory)
    }

    @Bean
    @ConditionalOnMissingBean(name = ["openMeteoRestClient"])
    fun openMeteoRestClient(openMeteoRestClientBuilder: RestClient.Builder): RestClient {
        return openMeteoRestClientBuilder.build()
    }
}
