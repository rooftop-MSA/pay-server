package org.rooftop.pay.infra.transaction

import org.rooftop.api.transaction.Transaction
import org.rooftop.api.transaction.TransactionState
import org.rooftop.pay.app.UndoPayment
import org.rooftop.pay.domain.PayRollbackEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptions
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Component
class PayTransactionListener(
    private val eventPublisher: ApplicationEventPublisher,
    @Qualifier("transactionServerConnectionFactory") private val connectionFactory: ReactiveRedisConnectionFactory,
    @Qualifier("payUndoServer") private val payUndoServer: ReactiveRedisTemplate<String, UndoPayment>,
) {

    private val options = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(java.time.Duration.ofMillis(100))
        .build()

    private val receiver = StreamReceiver.create(connectionFactory, options)

    @EventListener(PayTransactionJoinedEvent::class)
    fun subscribeStream(payTransactionJoinedEvent: PayTransactionJoinedEvent): Flux<Transaction> {
        return receiver.receive(StreamOffset.fromStart(payTransactionJoinedEvent.transactionId))
            .subscribeOn(Schedulers.boundedElastic())
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .dispatch()
    }

    private fun Flux<Transaction>.dispatch(): Flux<Transaction> {
        return this.filter { it.state == TransactionState.TRANSACTION_STATE_ROLLBACK }
            .flatMap { transaction ->
                payUndoServer.opsForValue()["PAY:${transaction.id}"]
                    .doOnNext { eventPublisher.publishEvent(PayRollbackEvent(it.id)) }
                    .map { transaction }
                    .flatMap {
                        payUndoServer.opsForValue().delete("PAY:${transaction.id}")
                            .map { transaction }
                            .retry()
                    }
            }
    }
}
