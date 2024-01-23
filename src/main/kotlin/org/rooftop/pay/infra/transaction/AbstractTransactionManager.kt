package org.rooftop.pay.infra.transaction

import org.rooftop.api.transaction.Transaction
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.pay.app.TransactionManager
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

abstract class AbstractTransactionManager<T>(
    private val transactionServerId: String,
    private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
) : TransactionManager<T> {

    override fun join(transactionId: String, state: T): Mono<String> {
        return joinOrStartTransaction()
            .undoBeforeState(state)
            .publishJoinedEvent()
            .contextWrite { it.put("transactionId", transactionId) }
    }

    protected fun joinOrStartTransaction(): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it["transactionId"]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    serverId = transactionServerId
                    state = TransactionState.TRANSACTION_STATE_JOIN
                })
            }
    }

    protected abstract fun Mono<String>.undoBeforeState(state: T): Mono<String>

    protected abstract fun Mono<String>.publishJoinedEvent(): Mono<String>

    override fun rollback(transactionId: String): Mono<Unit> {
        return findOpenedTransaction(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = transactionServerId
                state = TransactionState.TRANSACTION_STATE_ROLLBACK
            })
            .contextWrite { it.put("transactionId", transactionId) }
            .map { }
    }

    override fun commit(transactionId: String): Mono<Unit> {
        return findOpenedTransaction(transactionId)
            .publishTransaction(transaction {
                id = transactionId
                serverId = transactionServerId
                state = TransactionState.TRANSACTION_STATE_COMMIT
            })
            .contextWrite { it.put("transactionId", transactionId) }
            .map { }
    }

    private fun findOpenedTransaction(transactionId: String): Mono<String> {
        return transactionServer.opsForStream<String, ByteArray>()
            .range(transactionId, Range.open("-", "+"))
            .map { Transaction.parseFrom(it.value[DATA].toString().toByteArray()) }
            .filter { it.serverId == transactionServerId }
            .next()
            .switchIfEmpty(
                Mono.error {
                    IllegalStateException("Cannot find opened transaction id \"$transactionId\"")
                }
            )
            .transformTransactionId()
    }

    private fun Mono<String>.publishTransaction(transaction: Transaction): Mono<String> {
        return this.flatMap {
            publishTransaction(it, transaction)
        }
    }

    private fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return transactionServer.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(transactionId)
            )
            .transformTransactionId()
    }

    protected fun Mono<*>.transformTransactionId(): Mono<String> {
        return this.flatMap {
            Mono.deferContextual { Mono.just(it["transactionId"]) }
        }
    }

    private companion object {
        private const val DATA = "data"
    }
}
