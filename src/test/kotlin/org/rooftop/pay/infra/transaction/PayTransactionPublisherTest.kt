package org.rooftop.pay.infra.transaction

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.pay.app.TransactionPublisher
import org.rooftop.pay.app.UndoPayment
import org.rooftop.pay.domain.payment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@DisplayName("PayTransactionPublisher 클래스 의")
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        PayTransactionPublisher::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class PayTransactionPublisherTest(
    private val transactionPublisher: TransactionPublisher<UndoPayment>,
) : DescribeSpec({

    describe("join 메소드는") {
        context("undoServer 와 transactionServer 에 저장을 성공하면,") {

            val transactionId = "123"

            it("새로운 트랜잭션을 만들고, 생성된 트랜잭션의 id를 리턴한다.") {
                val result = transactionPublisher.join(transactionId, undoPayment)
                    .log()

                StepVerifier.create(result)
                    .assertNext {
                        it::class shouldBeEqual String::class
                    }
                    .verifyComplete()
            }
        }
    }

    describe("commit 메소드는") {
        context("transactionId에 해당하는 transaction에 join한 적이 있으면,") {

            val transactionId = "100"
            transactionPublisher.join(transactionId, undoPayment).block()

            it("transactionServer에 COMMIT 상태의 트랜잭션을 publish 한다.") {
                val result = transactionPublisher.commit(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = "999"

            it("IllegalStateException 을 던진다.") {
                val result = transactionPublisher.commit(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }

        }
    }

    describe("rollback 메소드는") {
        context("transactionId에 해당하는 transaction에 join한 적이 있으면,") {

            val transactionId = "789"
            transactionPublisher.join(transactionId, undoPayment).block()

            it("transactionServer에 ROLLBACK 상태의 트랜잭션을 publish 한다.") {
                val result = transactionPublisher.rollback(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = "1231234124"

            it("IllegalStateException을 던진다.") {
                val result = transactionPublisher.rollback(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }
        }
    }
}) {

    companion object {
        private val undoPayment = UndoPayment(payment().id)
    }
}