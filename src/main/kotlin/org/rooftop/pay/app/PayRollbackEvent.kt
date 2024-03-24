package org.rooftop.pay.app

data class PayRollbackEvent(
    val payId: Long,
    val orderId: Long,
)
