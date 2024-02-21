package org.rooftop.pay.app

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackHandler
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.PointService
import org.springframework.dao.OptimisticLockingFailureException
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@TransactionHandler
class TransactionHandler(
    private val payService: PayService,
    private val pointService: PointService,
) {

    @TransactionRollbackHandler
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent.undo)
            .map { parseReplay(it) }
            .dispatch()
    }

    private fun parseReplay(replay: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        replay.split(":")
            .forEach {
                println(it)
                val param = it.split("=")
                println("${param[0]} ${param[1]}")
                map[param[0]] = param[1]
            }
        return map
    }

    private fun Mono<Map<String, String>>.dispatch(): Mono<Unit> {
        return this.flatMap {
            when (it["type"]) {
                "pay-point" -> {
                    val id = it["id"]?.toLong()
                        ?: throw IllegalStateException("replay type \"pay-point\" must have \"id\" field")
                    val userId = it["userId"]?.toLong()
                        ?: throw IllegalStateException("replay type \"pay-point\" must have \"userId\" field")
                    val paidPoint = it["paidPoint"]?.toLong()
                        ?: throw IllegalStateException("replay type \"pay-point\" must have \"paidPoint\" field")

                    payService.rollbackPayment(id)
                        .retryWhen(retryOptimisticLockingFailure)
                        .flatMap {
                            pointService.rollbackPoint(userId, paidPoint)
                                .retryWhen(retryOptimisticLockingFailure)
                        }
                }

                "create-payment" -> {
                    val orderId = it["orderId"]?.toLong()
                        ?: throw IllegalStateException("replay type \"create-payment\" must have \"userId\" field\"")

                    payService.rollbackCreatePayment(orderId)
                        .retryWhen(retryOptimisticLockingFailure)
                }

                else -> error("Cannot find matched type \"${it["type"]}\"")
            }
        }.map { }
    }

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
