package org.rooftop.pay.domain

import org.rooftop.api.pay.PayRegisterOrderReq
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class PayService(
    private val idGenerator: IdGenerator,
    private val paymentRepository: PaymentRepository,
) {

    @Transactional
    fun createPayment(payRegisterOrderReq: PayRegisterOrderReq): Mono<Payment> {
        return Mono.just(payRegisterOrderReq)
            .map {
                Payment(
                    id = idGenerator.generate(),
                    userId = payRegisterOrderReq.userId,
                    orderId = payRegisterOrderReq.orderId,
                    price = payRegisterOrderReq.price,
                    isNew = true
                )
            }
            .flatMap { paymentRepository.save(it) }
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot register order \"$payRegisterOrderReq\"")
                }
            )
    }

    @Transactional
    @EventListener(PayRollbackEvent::class)
    fun rollbackPayment(payRollbackEvent: PayRollbackEvent): Mono<Unit> {
        return paymentRepository.findById(payRollbackEvent.id)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exist pay \"${payRollbackEvent.id}\"")
                }
            )
            .map { it.fail() }
            .flatMap {
                paymentRepository.save(it)
            }
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot update pay \"${payRollbackEvent.id}\" to fail")
                }
            )
            .map { }
    }

    fun getByOrderId(orderId: Long): Mono<Payment> {
        return paymentRepository.findByOrderId(orderId)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists payment by order-id \"$orderId\"")
                }
            )
    }
}
