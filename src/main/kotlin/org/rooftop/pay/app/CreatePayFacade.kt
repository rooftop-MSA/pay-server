package org.rooftop.pay.app

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.springframework.stereotype.Service
import org.springframework.transaction.CannotCreateTransactionException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
class CreatePayFacade(private val payService: PayService) {

    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return payService.createPayment(
            payRegisterOrderReq.userId,
            payRegisterOrderReq.orderId,
            payRegisterOrderReq.price,
        ).retryWhen(Retry.fixedDelay(Long.MAX_VALUE, 1000.milliseconds.toJavaDuration())
            .jitter(1.0)
            .filter { it is CannotCreateTransactionException })
    }
}
