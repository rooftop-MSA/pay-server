package org.rooftop.pay.app

import org.rooftop.api.identity.UserGetByTokenRes
import org.rooftop.api.pay.PayPointReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.order.app.event.PayConfirmEvent
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.Payment
import org.rooftop.pay.domain.PointService
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
class PayWithPointFacade(
    private val payService: PayService,
    private val pointService: PointService,
    private val transactionManager: TransactionManager,
    private val identityWebClient: WebClient,
) {

    fun payWithPoint(token: String, payPointReq: PayPointReq): Mono<Unit> {
        return existsUser(token)
            .getPaymentByOrderId(payPointReq)
            .registerPointIfNewUser()
            .startTransaction()
            .payWithPoint()
            .successPay()
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
            transactionManager.start(
                undo = UndoPayWithPoint(payment.id, payment.userId, payment.price),
                event = PayConfirmEvent(
                    payment.id,
                    payment.orderId,
                    "success",
                    payment.price,
                )
            ).map { it to payment }
        }
    }

    private fun Mono<Pair<String, Payment>>.payWithPoint(): Mono<Pair<String, Payment>> {
        return this.flatMap { (transactionId, payment) ->
            pointService.payWithPoint(payment.userId, payment.price)
                .retryWhen(retryOptimisticLockingFailure)
                .map { transactionId to payment }
                .rollbackOnError(transactionId)
        }
    }

    private fun Mono<Pair<String, Payment>>.successPay(): Mono<String> {
        return this.flatMap { (transactionId, payment) ->
            payService.successPayment(payment.id)
                .retryWhen(retryOptimisticLockingFailure)
                .map { transactionId }
                .rollbackOnError(transactionId)
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

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
