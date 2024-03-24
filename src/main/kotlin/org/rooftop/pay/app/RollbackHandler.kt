package org.rooftop.pay.app

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackListener
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Point
import org.rooftop.pay.domain.PointService
import org.springframework.dao.OptimisticLockingFailureException
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@TransactionHandler
class RollbackHandler(
    private val payService: PayService,
    private val pointService: PointService,
) {

    @TransactionRollbackListener(
        event = PayRollbackEvent::class,
        noRetryFor = [IllegalStateException::class]
    )
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Point> {
        return Mono.just(transactionRollbackEvent.decodeUndo(UndoPayWithPoint::class))
            .flatMap { undoPayWithPoint ->
                payService.rollbackPayment(undoPayWithPoint.id)
                    .retryWhen(retryOptimisticLockingFailure)
                    .flatMap {
                        pointService.rollbackPoint(
                            undoPayWithPoint.userId,
                            undoPayWithPoint.paidPoint
                        ).retryWhen(retryOptimisticLockingFailure)
                    }
            }
    }

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
