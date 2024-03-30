package org.rooftop.pay.core

import org.rooftop.netx.api.OrchestratorFactory
import org.rooftop.pay.core.Idempotent.Companion.IDEMPOTENT_KEY
import reactor.core.publisher.Mono

class IdempotentFactory(
    private val idempotentCache: IdempotentCache,
    private val orchestratorFactory: OrchestratorFactory,
) {

    fun <T : Any, V : Any> createWithRetryable(
        name: String,
        idempotentFunc: (T) -> Mono<V>,
    ): Idempotent<T, V> {
        val orchestrator = orchestratorFactory.create<T>(name)
            .startReactiveWithContext(
                contextOrchestrate = { context, request ->
                    Mono.fromCallable {
                        context.decodeContext(IDEMPOTENT_KEY, String::class)
                    }.flatMap { idempotentValue ->
                        idempotentCache.setInProgress(idempotentValue)
                            .map {
                                require(it) {
                                    "Duplicated api call by key \"$idempotentValue\""
                                }
                            }
                    }.map { request }
                }
            )
            .joinReactiveWithContext(
                contextOrchestrate = { _, request -> idempotentFunc.invoke(request) },
                contextRollback = { context, _ ->
                    idempotentCache.delete(context.decodeContext(IDEMPOTENT_KEY, String::class))
                }
            )
            .commitReactiveWithContext(
                contextOrchestrate = { context, request ->
                    idempotentCache.setDone(context.decodeContext(IDEMPOTENT_KEY, String::class))
                        .map { request }
                },
                contextRollback = { context, _ ->
                    idempotentCache.delete(context.decodeContext(IDEMPOTENT_KEY, String::class))
                }
            )
        return Idempotent(orchestrator)
    }
}
