package org.rooftop.pay.domain

data class PayRollbackEvent(
    val id: Long,
    val userId: Long,
    val paidPoint: Long,
)
