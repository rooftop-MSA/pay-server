package org.rooftop.pay.app

data class UndoPayWithPoint(
    val id: Long,
    val userId: Long,
    val paidPoint: Long,
)
