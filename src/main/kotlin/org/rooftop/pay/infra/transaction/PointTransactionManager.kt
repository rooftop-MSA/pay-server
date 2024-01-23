package org.rooftop.pay.infra.transaction

import org.rooftop.pay.app.UndoPoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PointTransactionManager(
    private val eventPublisher: ApplicationEventPublisher,
    @Value("\${distributed.transaction.server.id}") private val transactionServerId: String,
    @Qualifier("transactionServer") private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
    @Qualifier("pointUndoServer") private val pointUndoServer: ReactiveRedisTemplate<String, UndoPoint>,
) : AbstractTransactionManager<UndoPoint>(transactionServerId, transactionServer) {

    override fun Mono<String>.undoBeforeState(state: UndoPoint): Mono<String> {
        return this.flatMap { transactionId ->
            pointUndoServer.opsForValue().set("POINT:$transactionId", state)
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
            eventPublisher.publishEvent(PointTransactionJoinedEvent(it))
        }
    }
}
