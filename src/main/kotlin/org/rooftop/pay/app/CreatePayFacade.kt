package org.rooftop.pay.app

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
class CreatePayFacade(
    private val payService: PayService,
    private val transactionManager: TransactionManager,
) {

    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return joinTransaction(
            payRegisterOrderReq.transactionId,
            "type=create-payment:orderId=${payRegisterOrderReq.orderId}"
        ).flatMap {
            payService.createPayment(payRegisterOrderReq)
                .retryWhen(retryOptimisticLockingFailure)
        }.doOnError {
            transactionManager.rollback(
                payRegisterOrderReq.transactionId,
                it.message ?: it::class.simpleName!!
            ).subscribeOn(Schedulers.parallel()).subscribe()
            throw it
        }.doOnSuccess {
            transactionManager.commit(payRegisterOrderReq.transactionId)
                .subscribeOn(Schedulers.parallel())
                .subscribe()
        }
    }

    private fun joinTransaction(
        transactionId: String,
        undo: String,
    ): Mono<String> {
        return transactionManager.join(transactionId, undo)
    }

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
