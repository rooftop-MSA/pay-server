package org.rooftop.pay.app

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import org.rooftop.api.transaction.Transaction
import org.springframework.boot.test.context.TestComponent
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate

@TestComponent
class RedisAssertions(
    private val undoServer: ReactiveRedisTemplate<String, UndoPayment>,
    private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
) {

    fun assertUndoPaymentExist(key: String) {
        val exist = undoServer.opsForValue()["PAY:$key"].block()
        exist!! shouldNotBe null
    }

    fun assertTransactionServer(key: String, expected: Transaction) {
        val exist = transactionServer.opsForStream<String, ByteArray>()
            .range(key, Range.open("-", "+"))
            .map { Transaction.parseFrom(it.value["data"].toString().toByteArray()) }
            .blockFirst()

        exist shouldBeEqual expected
    }
}
