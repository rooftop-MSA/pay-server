package org.rooftop.pay.app

import org.rooftop.netx.api.SagaRollbackEvent
import org.rooftop.netx.api.SagaRollbackListener
import org.rooftop.netx.meta.SagaHandler
import org.rooftop.pay.core.Idempotent
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Point
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.CannotCreateTransactionException
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@SagaHandler
class PointRollbackHandler(
    private val payService: PayService,
) {

    lateinit var idempotentRollbackPoint: Idempotent<PayRollbackEvent, Point>


    @SagaRollbackListener(event = PayRollbackEvent::class)
    fun handleTransactionRollbackEvent(sagaRollbackEvent: SagaRollbackEvent): Mono<Point> {
        return Mono.just(sagaRollbackEvent.decodeEvent(PayRollbackEvent::class))
            .flatMap { payRollbackEvent ->
                payService.rollbackPayment(payRollbackEvent.payId)
                    .retryWhen(retryOptimisticLockingFailure)
                    .flatMap {
                        idempotentRollbackPoint.call(
                            sagaRollbackEvent.id,
                            payRollbackEvent,
                            Point::class
                        )
                    }
            }
    }

    companion object {
        const val RETRY_MOST_100_PERCENT = 1.0

        val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 1000.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter {
                    it is OptimisticLockingFailureException || it is CannotCreateTransactionException
                }
    }
}
