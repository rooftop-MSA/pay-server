package org.rooftop.pay.core

import org.rooftop.pay.domain.IdempotentRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class IdempotentCacheSupports(
    private val idempotentRepository: IdempotentRepository,
    private val idempotentCache: IdempotentCache,
) {

    fun <T : Any> withIdempotent(uniqueKey: String, func: () -> Mono<T>): Mono<T> {
        return idempotentCache.isCached(uniqueKey)
            .filter { !it }
            .flatMap { isCached ->
                idempotentRepository.existsById(uniqueKey)
                    .filter { it }
                    .flatMap { idempotentCache.cache(uniqueKey) }
                    .switchIfEmpty(Mono.just(isCached))
            }
            .filter { !it }
            .flatMap { func.invoke() }
            .flatMap { response ->
                idempotentCache.cache(uniqueKey)
                    .map { response }
            }
    }
}
