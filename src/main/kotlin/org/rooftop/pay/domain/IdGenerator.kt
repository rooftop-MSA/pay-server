package org.rooftop.pay.domain

fun interface IdGenerator {

    fun generate(): Long
}
