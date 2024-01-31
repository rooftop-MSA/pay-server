package org.rooftop.pay.domain

data class PointRollbackEvent(
    val userId: Long,
    val paidPoint: Long,
)
