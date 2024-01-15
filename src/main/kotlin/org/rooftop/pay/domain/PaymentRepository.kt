package org.rooftop.pay.domain

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface PaymentRepository : R2dbcRepository<Payment, Long>
