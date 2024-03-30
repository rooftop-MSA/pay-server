package org.rooftop.pay.infra

import jakarta.annotation.PostConstruct
import org.rooftop.pay.app.PointRollbackHandler
import org.rooftop.pay.core.IdempotentFactory
import org.rooftop.pay.domain.PointService
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class PointIdempotentConfigurer(
    private val pointService: PointService,
    private val idempotentFactory: IdempotentFactory,
    private val pointRollbackHandler: PointRollbackHandler,
) {

    @PostConstruct
    fun idempotentRollbackPoint() {
        pointRollbackHandler.idempotentRollbackPoint =
            idempotentFactory.createWithRetryable("Idmepotent rollback point") { payRollbackEvent ->
                pointService.rollbackPoint(
                    payRollbackEvent.userId,
                    payRollbackEvent.paidPoint
                ).retryWhen(PointRollbackHandler.retryOptimisticLockingFailure)
                    .onErrorResume {
                        if (it is IllegalStateException) {
                            return@onErrorResume Mono.empty()
                        }
                        throw it
                    }
            }
    }

}
