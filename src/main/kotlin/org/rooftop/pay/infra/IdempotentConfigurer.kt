package org.rooftop.pay.infra

import org.rooftop.pay.core.IdempotentCache
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class IdempotentConfigurer(
    @Value("\${idempotent.host}") private val host: String,
    @Value("\${idempotent.port}") private val port: String,
    @Value("\${idempotent.password:0000}") private val password: String,
) {

    @Bean
    fun idempotentCache(): IdempotentCache = RedisIdempotentCache(idempotentReactiveRedisTemplate())

    @Bean("idempotentReactiveRedisTemplate")
    fun idempotentReactiveRedisTemplate(): ReactiveRedisTemplate<String, String> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = StringRedisSerializer()

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, String> =
            RedisSerializationContext.newSerializationContext(keySerializer)

        val context = builder.value(valueSerializer).build()

        return ReactiveRedisTemplate(idempotentRedisConnectionFactory(), context);
    }

    @Bean
    fun idempotentRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val port: String = System.getProperty("idempotent.port") ?: port

        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port.toInt()
        redisStandaloneConfiguration.password = RedisPassword.of(password)

        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }
}
