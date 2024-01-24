package org.rooftop.pay.infra.transaction

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.pay.app.UndoPoint
import org.rooftop.pay.domain.point
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@DisplayName("PointTransactionManager 클래스의")
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        PointTransactionManager::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class PointTransactionManagerTest(
    private val transactionManager: PointTransactionManager,
) : DescribeSpec({

    describe("join 메소드는") {
        context("undoServer 와 transactionServer 에 저장을 성공하면,") {

            val transactionId = "123"

            it("새로운 트랜잭션을 만들고, 생성된 트랜잭션의 id를 리턴한다.") {
                val result = transactionManager.join(transactionId, undoPoint)
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
            transactionManager.join(transactionId, undoPoint).block()

            it("transactionServer에 COMMIT 상태의 트랜잭션을 publish 한다.") {
                val result = transactionManager.commit(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = "999"

            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.commit(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }

        }
    }

    describe("rollback 메소드는") {
        context("transactionId에 해당하는 transaction에 join한 적이 있으면,") {

            val transactionId = "789"
            transactionManager.join(transactionId, undoPoint).block()

            it("transactionServer에 ROLLBACK 상태의 트랜잭션을 publish 한다.") {
                val result = transactionManager.rollback(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = "1231234124"

            it("IllegalStateException을 던진다.") {
                val result = transactionManager.rollback(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }
        }
    }
}) {

    companion object {
        private val undoPoint = UndoPoint(point().id, 1_000L)
    }
}
