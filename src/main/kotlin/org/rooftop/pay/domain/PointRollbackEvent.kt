package org.rooftop.pay.domain

data class PointRollbackEvent(
    val id: Long,
    val paidPoint: Long,
)
