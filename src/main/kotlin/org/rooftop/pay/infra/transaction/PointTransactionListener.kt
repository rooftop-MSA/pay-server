package org.rooftop.pay.infra.transaction

import org.rooftop.api.transaction.Transaction
import org.rooftop.api.transaction.TransactionState
import org.rooftop.pay.app.UndoPoint
import org.rooftop.pay.domain.PointRollbackEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Component
class PointTransactionListener(
    private val eventPublisher: ApplicationEventPublisher,
    @Qualifier("transactionServerConnectionFactory") private val connectionFactory: ReactiveRedisConnectionFactory,
    @Qualifier("pointUndoServer") private val pointUndoServer: ReactiveRedisTemplate<String, UndoPoint>,
) {

    @EventListener(PointTransactionJoinedEvent::class)
    fun subscribeStream(pointTransactionJoinedEvent: PointTransactionJoinedEvent) {
        val options = StreamReceiver.StreamReceiverOptions.builder()
            .pollTimeout(java.time.Duration.ofMillis(100))
            .build()

        val receiver = StreamReceiver.create(connectionFactory, options)

        receiver.receive(StreamOffset.fromStart(pointTransactionJoinedEvent.transactionId))
            .subscribeOn(Schedulers.boundedElastic())
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .dispatch()
            .subscribe()
    }

    private fun Flux<Transaction>.dispatch(): Flux<Transaction> {
        return this.filter { it.state == TransactionState.TRANSACTION_STATE_ROLLBACK }
            .flatMap { transaction ->
                pointUndoServer.opsForValue()["POINT:${transaction.id}"]
                    .doOnNext {
                        eventPublisher.publishEvent(PointRollbackEvent(it.id, it.paidPoint))
                    }
                    .map { transaction }
                    .flatMap {
                        pointUndoServer.opsForValue().delete("POINT:${transaction.id}")
                            .map { transaction }
                            .retry()
                    }
            }
    }
}
