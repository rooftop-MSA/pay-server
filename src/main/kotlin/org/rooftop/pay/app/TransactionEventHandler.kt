package org.rooftop.pay.app

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.pay.domain.CreatePayRollbackEvent
import org.rooftop.pay.domain.PayRollbackEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TransactionEventHandler(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @EventListener(TransactionRollbackEvent::class)
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent.replay)
            .map { parseReplay(it) }
            .dispatch()
    }

    private fun parseReplay(replay: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        replay.split(":")
            .forEach {
                val param = it.split("=")
                map[param[0]] = param[1]
            }
        return map
    }

    private fun Mono<Map<String, String>>.dispatch(): Mono<Unit> {
        return this.doOnNext {
            when (it["type"]) {
                "pay-point" -> applicationEventPublisher.publishEvent(
                    PayRollbackEvent(
                        it["id"]?.toLong()
                            ?: throw IllegalStateException("replay type \"pay-point\" must have \"id\" field"),
                        it["userId"]?.toLong()
                            ?: throw IllegalStateException("replay type \"pay-point\" must have \"userId\" field"),
                        it["paidPoint"]?.toLong()
                            ?: throw IllegalStateException("replay type \"pay-point\" must have \"paidPoint\" field")
                    )
                )

                "create-pay" -> applicationEventPublisher.publishEvent(
                    CreatePayRollbackEvent(
                        it["orderId"]?.toLong()
                            ?: throw IllegalStateException("replay type \"create-payment\" must have \"userId\" field\"")
                    )
                )
            }
        }.map { }
    }
}
