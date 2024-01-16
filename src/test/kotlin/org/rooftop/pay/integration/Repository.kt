package org.rooftop.pay.integration

import org.rooftop.pay.domain.Payment
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate

fun R2dbcEntityTemplate.clearAll() {
    this.delete(Payment::class.java)
        .all()
        .block()
}
