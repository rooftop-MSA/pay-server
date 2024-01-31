package org.rooftop.pay.domain

import org.rooftop.api.pay.PayRegisterOrderReq
import org.springframework.context.event.EventListener
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

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
    fun successPayment(id: Long): Mono<Payment> {
        return paymentRepository.findById(id)
            .map { it.success() }
            .flatMap { paymentRepository.save(it) }
            .retryWhen(retryOptimisticLockingFailure)
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
            .retryWhen(retryOptimisticLockingFailure)
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

    private companion object {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
