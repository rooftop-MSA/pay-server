package org.rooftop.pay.server

import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@TestComponent
class MockOrderServer : MockServer() {

    @Bean
    fun orderWebClient(): WebClient = WebClient.create(mockWebSerer.url("").toString())
}