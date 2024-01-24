package org.rooftop.pay.domain

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.api.pay.payRegisterOrderReq
import org.rooftop.pay.infra.TsidGenerator
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import kotlin.time.Duration.Companion.seconds

@DataR2dbcTest
@ContextConfiguration(
    classes = [
        R2dbcConfigurer::class,
        TsidGenerator::class,
        PayService::class,
    ]
)
@DisplayName("PayService 클래스의")
internal class PayServiceTest(
    private val payService: PayService,
    private val paymentRepository: PaymentRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DescribeSpec({

    describe("rollbackPay 메소드는") {
        context("PayRollbackEvent가 발행되면,") {
            val exist = payService.createPayment(payRegisterOrderReq).block()!!
            val event = PayRollbackEvent(exist.id)

            it("저장된 Payment를 Failed 상태로 변경한다.") {
                applicationEventPublisher.publishEvent(event)

                eventually(10.seconds) {
                    StepVerifier.create(paymentRepository.findById(event.id))
                        .assertNext {
                            it.state shouldBeEqual PaymentState.FAILED
                        }
                        .verifyComplete()
                }
            }
        }
    }
}) {
    private companion object {
        private val payRegisterOrderReq = payRegisterOrderReq {
            this.orderId = 999L
            this.userId = 1L
            this.price = 10_000
            this.transactionId = "123"
        }
    }
}
