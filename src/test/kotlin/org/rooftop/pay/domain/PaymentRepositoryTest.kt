package org.rooftop.pay.domain

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import org.rooftop.pay.infra.TsidGenerator
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DataR2dbcTest
@DisplayName("PaymentRepository 클래스의")
@ContextConfiguration(classes = [R2dbcConfigurer::class, TsidGenerator::class])
internal class PaymentRepositoryTest(
    private val idGenerator: IdGenerator,
    private val paymentRepository: PaymentRepository,
) : DescribeSpec({

    afterEach {
        paymentRepository.deleteAll().block()
    }

    describe("save 메소드는") {
        context("올바른 Payment 도메인이 들어오면,") {

            val payment = payment(id = idGenerator.generate(), orderId = idGenerator.generate())

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
            val existPayment = paymentRepository.save(
                payment(
                    id = idGenerator.generate(),
                    orderId = idGenerator.generate()
                )
            ).block()!!

            it("Payment를 반환한다.") {
                val result = paymentRepository.findById(existPayment.id)

                StepVerifier.create(result)
                    .assertNext {
                        it.shouldBeEqualToIgnoringFields(
                            existPayment,
                            Payment::createdAt,
                            Payment::modifiedAt
                        )
                    }
                    .verifyComplete()
            }
        }
    }
})
