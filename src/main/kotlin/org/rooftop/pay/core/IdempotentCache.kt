package org.rooftop.pay.core

import reactor.core.publisher.Mono

interface IdempotentCache {

    fun cache(key: String): Mono<Boolean>

    fun isCached(key: String): Mono<Boolean>
}
