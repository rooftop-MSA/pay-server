package org.rooftop.pay

import org.rooftop.netx.meta.EnableDistributedTransaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@EnableR2dbcAuditing
@SpringBootApplication
@EnableDistributedTransaction
@EnableAutoConfiguration(exclude = [RedisReactiveAutoConfiguration::class])
class Application {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
