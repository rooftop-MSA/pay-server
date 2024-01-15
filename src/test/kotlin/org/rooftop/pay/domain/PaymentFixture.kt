package org.rooftop.pay.domain

fun payment(
    id: Long = 1L,
    userId: Long = 2L,
    orderId: Long = 3L,
    price: Long = 10_000L,
    state: PaymentState = PaymentState.PENDING,
    isNew: Boolean = true,
): Payment = Payment(
    id = id,
    userId = userId,
    orderId = orderId,
    price = price,
    state = state,
    isNew = isNew,
)
