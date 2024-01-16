package org.rooftop.pay.controller

import org.rooftop.api.pay.PayRegisterOrderReq
import org.rooftop.pay.app.CreatePayFacade
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1/pays")
class PayController(
    private val createPayFacade: CreatePayFacade,
) {

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.OK)
    fun createPay(@RequestBody payRegisterOrderReq: PayRegisterOrderReq): Mono<Unit> {
        return createPayFacade.createPayment(payRegisterOrderReq)
            .map { }
    }
}
