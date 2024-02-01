package org.rooftop.pay.app

data class UndoPayment(
    val id: Long,
    val userId: Long,
    val paidPoint: Long,
)
