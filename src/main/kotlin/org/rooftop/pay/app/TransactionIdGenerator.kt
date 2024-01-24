package org.rooftop.pay.app

fun interface TransactionIdGenerator {

    fun generate(): String
}
