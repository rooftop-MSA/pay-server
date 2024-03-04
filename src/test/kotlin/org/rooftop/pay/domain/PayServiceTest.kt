package org.rooftop.pay.domain

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.pay.infra.TsidGenerator
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import kotlin.time.Duration.Companion.seconds

@DataR2dbcTest
@EnableR2dbcAuditing
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
) : DescribeSpec({

    describe("rollbackPay 메소드는") {
        context("PayRollbackEvent가 발행되면,") {
            val exist = payService.createPayment(USER_ID, ORDER_ID, PRICE).block()!!

            it("저장된 Payment를 Failed 상태로 변경한다.") {
                payService.rollbackPayment(exist.id).subscribe()

                eventually(10.seconds) {
                    StepVerifier.create(paymentRepository.findById(exist.id))
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
        private const val ORDER_ID = 999L
        private const val USER_ID = 1L
        private const val PRICE = 10_000L
    }
}
