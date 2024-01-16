package org.rooftop.pay.app

import com.fasterxml.jackson.annotation.JsonCreator

data class UndoPayment @JsonCreator constructor(
    val id: Long,
) {
}
