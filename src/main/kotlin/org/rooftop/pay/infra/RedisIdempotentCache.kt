package org.rooftop.pay.infra

import org.rooftop.pay.core.IdempotentCache
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisIdempotentCache(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) : IdempotentCache {

    override fun cache(key: String): Mono<Boolean> =
        reactiveRedisTemplate.opsForValue().setIfAbsent("idempotent:$key", CACHED)

    override fun delete(key: String): Mono<Unit> =
        reactiveRedisTemplate.delete("idempotent:$key").map { }

    private companion object {
        private const val CACHED = "Cached"
    }
}
