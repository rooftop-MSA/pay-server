package org.rooftop.pay.domain

import com.ninjasquad.springmockk.MockkClear
import com.ninjasquad.springmockk.clear
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DataR2dbcTest
@DisplayName("PaymentRepository 클래스의")
@ContextConfiguration(classes = [R2dbcConfigurer::class])
internal class PaymentRepositoryTest(
    private val paymentRepository: PaymentRepository,
) : DescribeSpec({

    afterEach {
        paymentRepository.deleteAll().block()
    }

    describe("save 메소드는") {
        context("올바른 Payment 도메인이 들어오면,") {
            it("Payment를 저장한다.") {
                val result = paymentRepository.save(payment)

                StepVerifier.create(result)
                    .assertNext {
                        it.shouldBeEqualToIgnoringFields(
                            payment,
                            Payment::createdAt,
                            Payment::modifiedAt
                        )
                    }
                    .verifyComplete()
            }
        }
    }

    describe("findById 메소드는") {
        context("저장된 Payment의 id가 들어오면,") {
            val existPayment = paymentRepository.save(payment).block()!!

            it("Payment를 반환한다.") {
                val result = paymentRepository.findById(existPayment.id)

                StepVerifier.create(result)
                    .assertNext {
                        it shouldBeEqualUsingFields existPayment
                    }
                    .verifyComplete()
            }
        }
    }
}) {

    companion object {
        private val payment = payment()
    }
}
