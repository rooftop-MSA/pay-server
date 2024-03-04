package org.rooftop.pay.app

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class CreatePayFacade(
    private val payService: PayService,
    private val transactionManager: TransactionManager,
) {

    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return transactionManager.exists(payRegisterOrderReq.transactionId)
            .flatMap {
                payService.createPayment(
                    payRegisterOrderReq.userId,
                    payRegisterOrderReq.orderId,
                    payRegisterOrderReq.price,
                ).doOnError {
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
    }
}
