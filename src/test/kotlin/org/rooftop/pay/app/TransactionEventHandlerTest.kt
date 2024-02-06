package org.rooftop.pay.app

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.pay.domain.CreatePayRollbackEvent
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import kotlin.time.Duration.Companion.seconds

@ContextConfiguration(classes = [TransactionEventHandler::class, EventCapture::class])
@DisplayName("TransactionEventHandler 클래스의")
internal class TransactionEventHandlerTest(
    private val transactionEventHandler: TransactionEventHandler,
    private val eventCapture: EventCapture,
) : DescribeSpec({

    beforeEach {
        eventCapture.clear()
    }

    describe("handleTransactionRollbackEvent 메소드는") {
        context("create-payment type의 TransactionRollbackEvent가 들어오면,") {
            val transactionRollbackEvent = TransactionRollbackEvent(
                transactionId = "1",
                undoState = "type=create-payment:orderId=1",
                cause = "",
                nodeName = ""
            )

            it("CreatePayRollbackEvent를 발행한다.") {
                val result =
                    transactionEventHandler.handleTransactionRollbackEvent(transactionRollbackEvent)

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()

                eventually(10.seconds) {
                    eventCapture.capturedCount(CreatePayRollbackEvent::class) shouldBe 1
                }
            }
        }
    }
}) {
}
