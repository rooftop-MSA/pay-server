package org.rooftop.pay.core

import reactor.core.publisher.Mono

interface IdempotentCache {

    fun setInProgress(key: String): Mono<Boolean>

    fun setDone(key: String): Mono<Boolean>

    fun delete(key: String): Mono<Unit>
}
