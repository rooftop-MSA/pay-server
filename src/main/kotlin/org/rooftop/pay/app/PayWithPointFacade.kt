package org.rooftop.pay.app

import org.rooftop.api.identity.UserGetByTokenRes
import org.rooftop.api.order.ConfirmState
import org.rooftop.api.order.orderConfirmReq
import org.rooftop.api.pay.PayPointReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.rooftop.pay.domain.PointService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class PayWithPointFacade(
    private val payService: PayService,
    private val pointService: PointService,
    private val transactionManager: TransactionManager,
    private val orderWebClient: WebClient,
    private val identityWebClient: WebClient,
) {

    fun payWithPoint(token: String, payPointReq: PayPointReq): Mono<Unit> {
        return existsUser(token)
            .getPaymentByOrderId(payPointReq)
            .registerPointIfNewUser()
            .startTransaction()
            .payWithPoint()
            .successPay()
            .commitOnSuccess()
            .publishOn(Schedulers.parallel())
            .confirmOrder(payPointReq)
            .map { }
    }

    private fun existsUser(token: String): Mono<UserGetByTokenRes> {
        return identityWebClient.get()
            .uri("/v1/users/tokens")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchangeToMono {
                if (it.statusCode().is2xxSuccessful) {
                    return@exchangeToMono it.bodyToMono(UserGetByTokenRes::class.java)
                }
                it.createError()
            }
    }

    private fun Mono<UserGetByTokenRes>.getPaymentByOrderId(payPointReq: PayPointReq): Mono<Payment> {
        return this.flatMap { userGetByTokenRes ->
            payService.getByOrderId(payPointReq.orderId)
                .filter {
                    if (it.userId == userGetByTokenRes.id) {
                        return@filter true
                    }
                    throw IllegalArgumentException("Not matched userId \"${userGetByTokenRes.id}\"")
                }
        }
    }

    private fun Mono<Payment>.registerPointIfNewUser(): Mono<Payment> {
        return this.flatMap { payment ->
            pointService.exists(payment.userId)
                .filter { it == false }
                .flatMap { pointService.createPoint(payment.userId) }
                .map { payment }
                .switchIfEmpty(Mono.just(payment))
        }
    }

    private fun Mono<Payment>.startTransaction(): Mono<Pair<String, Payment>> {
        return this.flatMap { payment ->
            transactionManager.start("type=pay-point:id=${payment.id}:userId=${payment.userId}:paidPoint=${payment.price}")
                .map {
                    it to payment
                }
        }
    }

    private fun Mono<Pair<String, Payment>>.payWithPoint(): Mono<Pair<String, Payment>> {
        return this.flatMap { (transactionId, payment) ->
            pointService.payWithPoint(payment.userId, payment.price)
                .map { transactionId to payment }
                .rollbackOnError(transactionId)
        }
    }

    private fun Mono<Pair<String, Payment>>.successPay(): Mono<String> {
        return this.flatMap { (transactionId, payment) ->
            payService.successPayment(payment.id)
                .map { transactionId }
                .rollbackOnError(transactionId)
        }
    }

    private fun Mono<String>.confirmOrder(payPointReq: PayPointReq): Mono<String> {
        return this.flatMap { transactionId ->
            orderWebClient.post()
                .uri("/v1/orders/confirms")
                .header(HttpHeaders.CONTENT_TYPE, "application/x-protobuf")
                .bodyValue(orderConfirmReq {
                    this.orderId = payPointReq.orderId
                    this.confirmState = ConfirmState.CONFIRM_STATE_SUCCESS
                    this.transactionId = transactionId
                }).exchangeToMono {
                    if (it.statusCode().is2xxSuccessful) {
                        return@exchangeToMono Mono.just(transactionId)
                    }
                    it.createError()
                }
        }
    }

    private fun Mono<String>.commitOnSuccess(): Mono<String> {
        return this.doOnSuccess { transactionId ->
            transactionManager.commit(transactionId)
                .subscribeOn(Schedulers.parallel())
                .subscribe()
        }
    }

    private fun <T> Mono<T>.rollbackOnError(transactionId: String): Mono<T> {
        return this.doOnError {
            transactionManager.rollback(transactionId, it.message ?: it::class.simpleName!!)
                .subscribeOn(Schedulers.parallel())
                .subscribe()
            throw it
        }
    }
}
