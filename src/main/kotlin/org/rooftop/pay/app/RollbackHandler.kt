package org.rooftop.pay.app

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackListener
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Point
import org.rooftop.pay.domain.PointService
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.CannotCreateTransactionException
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@TransactionHandler
class RollbackHandler(
    private val payService: PayService,
    private val pointService: PointService,
) {

    @TransactionRollbackListener(event = PayRollbackEvent::class)
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Point> {
        return Mono.just(transactionRollbackEvent.decodeEvent(PayRollbackEvent::class))
            .flatMap { payRollbackEvent ->
                payService.rollbackPayment(payRollbackEvent.payId)
                    .retryWhen(retryOptimisticLockingFailure)
                    .flatMap {
                        pointService.rollbackPoint(
                            payRollbackEvent.userId,
                            payRollbackEvent.paidPoint
                        ).retryWhen(retryOptimisticLockingFailure)
                            .onErrorResume {
                                if (it is IllegalStateException) {
                                    return@onErrorResume Mono.empty()
                                }
                                throw it
                            }
                    }
            }
    }

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 1000.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter {
                    it is OptimisticLockingFailureException || it is CannotCreateTransactionException
                }
    }
}
