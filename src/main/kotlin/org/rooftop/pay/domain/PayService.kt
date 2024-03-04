package org.rooftop.pay.domain

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
    fun createPayment(userId: Long, orderId: Long, totalPrice: Long): Mono<Payment> {
        return Mono.just(
            Payment(
                id = idGenerator.generate(),
                userId = userId,
                orderId = orderId,
                price = totalPrice,
                isNew = true
            )
        ).flatMap {
            paymentRepository.save(it)
        }.switchIfEmpty(
            Mono.error {
                throw IllegalArgumentException("Cannot register order \"$orderId\"")
            }
        )
    }

    @Transactional
    fun successPayment(id: Long): Mono<Payment> {
        return paymentRepository.findById(id)
            .map { it.success() }
            .flatMap { paymentRepository.save(it) }
    }

    @Transactional
    fun rollbackPayment(id: Long): Mono<Payment> {
        return paymentRepository.findById(id)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Cannot find exist pay \"$id\"")
                }
            )
            .map { it.fail() }
            .flatMap {
                paymentRepository.save(it)
            }
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Cannot update pay \"$id\" to fail")
                }
            )
    }

    @Transactional
    fun rollbackCreatePayment(orderId: Long): Mono<Payment> {
        return paymentRepository.findByOrderId(orderId)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Cannot find exist pay \"$orderId\"")
                }
            )
            .map { it.fail() }
            .flatMap { paymentRepository.save(it) }
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
