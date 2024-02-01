package org.rooftop.pay.infra.transaction

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.pay.app.UndoPayment
import org.rooftop.pay.domain.PayRollbackEvent
import org.rooftop.pay.domain.payment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@DisplayName("PayTransactionListenerTest 클래스 의")
@ContextConfiguration(
    classes = [
        EventCapture::class,
        RedisContainer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        TransactionManager::class,
        TransactionListener::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class PayTransactionListenerTest(
    private val eventCapture: EventCapture,
    private val transactionPublisher: TransactionManager,
) : DescribeSpec({

    afterEach { eventCapture.clear() }

    describe("subscribeStream 메소드는") {
        context("rollback transaction 이 들어오면,") {

            val transactionId = "123"

            it("PayRollbackEvent 를 발행한다.") {
                transactionPublisher.join(transactionId, undoPayment).block()
                transactionPublisher.rollback(transactionId).block()

                eventually(10.seconds) {
                    eventCapture.capturedCount(PayRollbackEvent::class) shouldBeEqual 1
                }
            }
        }

        context("여러개의 transactionId가 등록되어도 ") {

            val transactionId1 = "456"
            val transactionId2 = "789"

            it("동시에 요청을 읽을 수 있다.") {
                transactionPublisher.join(transactionId1, undoPayment).block()
                transactionPublisher.join(transactionId2, undoPayment).block()

                transactionPublisher.rollback(transactionId1).block()
                transactionPublisher.rollback(transactionId2).block()

                eventually(10.seconds) {
                    eventCapture.capturedCount(PayRollbackEvent::class) shouldBeEqual 2
                }
            }
        }
    }

}) {
    private companion object {
        private val payment = payment()
        private val undoPayment = UndoPayment(payment.id, payment.userId, payment.price)
    }
}
