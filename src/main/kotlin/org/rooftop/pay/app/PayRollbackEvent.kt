package org.rooftop.pay.app

data class PayRollbackEvent(
    val payId: Long,
    val userId: Long,
    val orderId: Long,
    val paidPoint: Long,
)
