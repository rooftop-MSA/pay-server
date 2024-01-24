package org.rooftop.pay.controller

import org.rooftop.api.pay.PayPointReq
import org.rooftop.pay.app.PayWithPointFacade
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class PointController(
    private val payWithPointFacade: PayWithPointFacade,
) {

    @PostMapping("/v1/pays/points")
    @ResponseStatus(HttpStatus.OK)
    fun payWithPoint(
        @RequestHeader(HttpHeaders.AUTHORIZATION) token: String,
        @RequestBody payPointReq: PayPointReq,
    ): Mono<Unit> = payWithPointFacade.payWithPoint(token, payPointReq)
}
