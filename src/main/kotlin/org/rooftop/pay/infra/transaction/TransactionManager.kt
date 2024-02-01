package org.rooftop.pay.infra.transaction

import org.rooftop.pay.app.UndoPayment
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TransactionManager(
    private val eventPublisher: ApplicationEventPublisher,
    @Value("\${distributed.transaction.server.id}") private val transactionServerId: String,
    @Qualifier("transactionServer") private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
    private val undoServer: ReactiveRedisTemplate<String, UndoPayment>,
) : AbstractTransactionManager<UndoPayment>(transactionServerId, transactionServer) {

    override fun Mono<String>.undoBeforeState(state: UndoPayment): Mono<String> {
        return this.flatMap { transactionId ->
            undoServer.opsForValue().set("PAY:$transactionId", state)
                .flatMap {
                    when (it) {
                        true -> Mono.just(it)
                        false -> Mono.error {
                            IllegalStateException("error occurred cause set undo fail")
                        }
                    }
                }
                .transformTransactionId()
        }
    }

    override fun Mono<String>.publishJoinedEvent(): Mono<String> {
        return this.doOnSuccess {
            eventPublisher.publishEvent(TransactionJoinedEvent(it))
        }
    }
}
