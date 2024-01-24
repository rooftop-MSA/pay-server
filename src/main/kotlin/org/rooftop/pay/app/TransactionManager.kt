package org.rooftop.pay.app

import reactor.core.publisher.Mono

interface TransactionManager<T> {

    fun join(transactionId: String, state: T): Mono<String>

    fun commit(transactionId: String): Mono<Unit>

    fun rollback(transactionId: String): Mono<Unit>

}
