package org.rooftop.pay.domain

data class CreatePayRollbackEvent(
    val orderId: Long,
)
