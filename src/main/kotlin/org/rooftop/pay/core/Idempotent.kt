package org.rooftop.pay.core

import org.rooftop.netx.api.Orchestrator
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

class Idempotent<T : Any, V : Any>(
    private val orchestrator: Orchestrator<T, V>
) {

    fun call(uniqueId: String, request: T, returnType: KClass<V>): Mono<V> {
        return orchestrator.saga(request, mutableMapOf(IDEMPOTENT_KEY to uniqueId))
            .map { it.decodeResultOrThrow(returnType) }
    }

    companion object {
        const val IDEMPOTENT_KEY = "i_key"
    }
}
