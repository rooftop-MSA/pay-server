package org.rooftop.pay.infra

import org.rooftop.pay.core.IdempotentCache
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisIdempotentCache(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) : IdempotentCache {

    override fun cache(key: String): Mono<Boolean> =
        reactiveRedisTemplate.opsForValue().setIfAbsent("idempotent:$key", CACHED)

    override fun isCached(key: String): Mono<Boolean> =
        reactiveRedisTemplate.opsForValue()["idempotent:$key"]
            .switchIfEmpty(
                Mono.just("false")
            ).map {
                when (it) {
                    "false" -> false
                    else -> true
                }
            }

    private companion object {
        private const val CACHED = "Cached"
    }
}
