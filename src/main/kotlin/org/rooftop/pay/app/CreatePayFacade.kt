package org.rooftop.pay.app

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CreatePayFacade(private val payService: PayService) {

    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return payService.createPayment(
            payRegisterOrderReq.userId,
            payRegisterOrderReq.orderId,
            payRegisterOrderReq.price,
        )
    }
}
