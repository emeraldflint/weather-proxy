package com.tequipy.weather.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestClient

@TestConfiguration
class TestRestClientConfig {

    @Bean
    fun mockRestServiceServer(openMeteoRestClientBuilder: RestClient.Builder): MockRestServiceServer {
        return MockRestServiceServer.bindTo(openMeteoRestClientBuilder).build()
    }

    @Bean
    fun openMeteoRestClient(
        openMeteoRestClientBuilder: RestClient.Builder,
        @Suppress("UNUSED_PARAMETER") mockRestServiceServer: MockRestServiceServer
    ): RestClient {
        return openMeteoRestClientBuilder.build()
    }
}
