package org.rooftop.pay.app

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToUsingFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.rooftop.netx.api.SagaManager
import org.rooftop.pay.Application
import org.rooftop.pay.domain.*
import org.rooftop.pay.server.MockIdentityServer
import org.springframework.boot.test.context.SpringBootTest
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration.Companion.seconds

@DisplayName("PayRollbackHandler의")
@SpringBootTest(
    classes = [
        Application::class,
        R2dbcConfigurer::class,
        MockIdentityServer::class,
        RedisContainer::class,
    ]
)
internal class PayRollbackHandlerTest(
    private val sagaManager: SagaManager,
    private val paymentRepository: PaymentRepository,
    private val pointRepository: PointRepository,
) : DescribeSpec({

    beforeEach {
        paymentRepository.save(payment).block()
        pointRepository.save(point).block()
    }

    afterEach {
        paymentRepository.deleteAll().block()
        pointRepository.deleteAll().block()
    }

    describe("handlePayRollbackEvent 메소드는") {
        context("PayRollbackEvent를 받으면,") {
            val sagaId = sagaManager.startSync()

            it("pay를 FAILED로 변경하고, point를 복구한다.") {
                sagaManager.rollbackSync(sagaId, "for test", payRollbackEvent)

                eventually(5.seconds) {
                    val payment = paymentRepository.findById(payment.id).block()!!
                    val point = pointRepository.findById(point.id).block()!!

                    payment.state shouldBe PaymentState.FAILED
                    point.point shouldBeEqual 2_000L
                }
            }
        }
    }
}) {

    private companion object {
        private val payment = payment(price = 1_000L)
        private val point = point(point = 1_000L)
        private val payRollbackEvent = PayRollbackEvent(
            payment.id,
            payment.userId,
            payment.orderId,
            payment.price
        )
    }
}
