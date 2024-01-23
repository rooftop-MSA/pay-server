package org.rooftop.pay.app

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CreatePayFacade(
    private val payService: PayService,
    private val transactionPublisher: TransactionPublisher<UndoPayment>,
) {

    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return payService.createPayment(payRegisterOrderReq)
            .joinTransaction(payRegisterOrderReq.transactionId)
            .doOnError {
                transactionPublisher.rollback(payRegisterOrderReq.transactionId)
                throw it
            }
    }

    private fun Mono<Payment>.joinTransaction(transactionId: String): Mono<Payment> {
        return this.flatMap { payment ->
            transactionPublisher.join(transactionId, UndoPayment(payment.id))
                .map { payment }
        }
    }
}
