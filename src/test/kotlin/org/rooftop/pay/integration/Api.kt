package org.rooftop.pay.integration

import org.rooftop.api.pay.PayPointReq
import org.rooftop.api.pay.PayRegisterOrderReq
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec

fun WebTestClient.createPay(payRegisterOrderReq: PayRegisterOrderReq): ResponseSpec {
    return this.post()
        .uri("/v1/pays/orders")
        .header(HttpHeaders.CONTENT_TYPE, "application/x-protobuf")
        .bodyValue(payRegisterOrderReq.toByteArray())
        .exchange()
}

fun WebTestClient.payPoint(token: String, payPointReq: PayPointReq): ResponseSpec {
    return this.post()
        .uri("/v1/pays/points")
        .header(HttpHeaders.CONTENT_TYPE, "application/x-protobuf")
        .header(HttpHeaders.AUTHORIZATION, token)
        .bodyValue(payPointReq.toByteArray())
        .exchange()
}
