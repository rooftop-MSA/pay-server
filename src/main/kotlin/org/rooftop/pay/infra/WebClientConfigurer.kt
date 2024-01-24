package org.rooftop.pay.infra

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@Profile("prod")
class WebClientConfigurer {

    @Bean
    fun orderWebClient(): WebClient = WebClient.create("http://order.rooftop.org")

    @Bean
    fun identityWebClient(): WebClient = WebClient.create("http://identity.rooftop.org")
}
