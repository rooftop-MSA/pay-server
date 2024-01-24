package org.rooftop.pay.domain

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface PaymentRepository : R2dbcRepository<Payment, Long> {

    fun findByOrderId(orderId: Long): Mono<Payment>
}
