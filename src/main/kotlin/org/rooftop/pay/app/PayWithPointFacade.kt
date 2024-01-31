package org.rooftop.pay.app

import org.rooftop.api.identity.UserGetByTokenRes
import org.rooftop.api.order.ConfirmState
import org.rooftop.api.order.orderConfirmReq
import org.rooftop.api.pay.PayPointReq
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
    private val transactionIdGenerator: TransactionIdGenerator,
    private val pointTransactionManager: TransactionManager<UndoPoint>,
    private val payTransactionManager: TransactionManager<UndoPayment>,
    private val orderWebClient: WebClient,
    private val identityWebClient: WebClient,
) {

    fun payWithPoint(token: String, payPointReq: PayPointReq): Mono<Unit> {
        return Mono.deferContextual<String> { Mono.just(it["transactionId"]) }
            .existsUser(token)
            .getPaymentByOrderId(payPointReq)
            .registerPointIfNewUser()
            .startPointTransaction()
            .payWithPoint()
            .startPayTransaction()
            .successPay()
            .commitOnSuccess()
            .confirmOrder(payPointReq)
            .contextWrite { it.put("transactionId", transactionIdGenerator.generate()) }
            .map { }
    }

    private fun Mono<String>.existsUser(token: String): Mono<UserGetByTokenRes> {
        return this.flatMap {
            identityWebClient.get()
                .uri("/v1/users/tokens")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchangeToMono {
                    if (it.statusCode().is2xxSuccessful) {
                        return@exchangeToMono it.bodyToMono(UserGetByTokenRes::class.java)
                    }
                    it.createError()
                }
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

    private fun Mono<Payment>.startPointTransaction(): Mono<Payment> {
        return this.withTransactionId()
            .flatMap { (transactionId, payment) ->
                pointTransactionManager.join(
                    transactionId,
                    UndoPoint(payment.userId, payment.price)
                ).map {
                    payment
                }.rollbackOnError(transactionId)
            }
    }

    private fun Mono<Payment>.payWithPoint(): Mono<Payment> {
        return this.withTransactionId()
            .flatMap { (transactionId, payment) ->
                pointService.payWithPoint(payment.userId, payment.price)
                    .map { payment }
                    .rollbackOnError(transactionId)
            }
    }

    private fun Mono<Payment>.startPayTransaction(): Mono<Payment> {
        return this.withTransactionId()
            .flatMap { (transactionId, payment) ->
                payTransactionManager.join(transactionId, UndoPayment(payment.id))
                    .rollbackOnError(transactionId)
                    .map { payment }
            }
    }

    private fun Mono<Payment>.successPay(): Mono<String> {
        return this.withTransactionId()
            .flatMap { (transactionId, payment) ->
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
            pointTransactionManager.commit(transactionId)
                .flatMap { payTransactionManager.commit(transactionId) }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }
    }

    private fun <T> Mono<T>.rollbackOnError(transactionId: String): Mono<T> {
        return this.doOnError {
            pointTransactionManager.rollback(transactionId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
            throw it
        }
    }

    private fun <T> Mono<T>.withTransactionId(): Mono<Pair<String, T>> {
        return this.flatMap { cascadeArgument ->
            Mono.deferContextual<String> { Mono.just(it["transactionId"]) }
                .map { it to cascadeArgument }
        }
    }
}
