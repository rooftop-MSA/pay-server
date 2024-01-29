package org.rooftop.pay.domain

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import org.rooftop.pay.infra.TsidGenerator
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DataR2dbcTest
@EnableR2dbcAuditing
@ContextConfiguration(
    classes = [
        R2dbcConfigurer::class,
        TsidGenerator::class,
        PointService::class,
    ]
)
@DisplayName("PointService 클래스의")
internal class PointServiceTest(
    private val pointService: PointService,
    private val idGenerator: IdGenerator,
) : DescribeSpec({

    describe("createPoint 메소드는") {
        context("중복된 userId가 저장되어있지 않다면,") {

            val userId = idGenerator.generate()
            val expected = point(userId = userId)

            it("새로운 Point를 저장하는데 성공한다.") {
                val result = pointService.createPoint(userId)

                StepVerifier.create(result)
                    .assertNext {
                        it.shouldBeEqualToIgnoringFields(
                            expected,
                            Point::id,
                            Point::createdAt,
                            Point::modifiedAt
                        )
                    }
                    .verifyComplete()
            }
        }

        context("중복된 userId가 저장되어 있다면,") {

            val duplicateUserId = idGenerator.generate()
            pointService.createPoint(duplicateUserId).block()

            it("저장에 실패하고 에러를 던진다.") {
                val result = pointService.createPoint(duplicateUserId)

                StepVerifier.create(result)
                    .verifyError()
            }
        }
    }

    describe("payWithPoint 메소드는") {
        context("point 를 차감할 수 있다면,") {

            val userId = idGenerator.generate()
            val expected = point(userId = userId, point = 0)
            pointService.createPoint(userId).block()

            it("point 가 차감된 결과를 반환한다.") {
                val result = pointService.payWithPoint(userId, 1_000)

                StepVerifier.create(result)
                    .assertNext {
                        it.shouldBeEqualToIgnoringFields(
                            expected,
                            Point::id,
                            Point::createdAt,
                            Point::modifiedAt
                        )
                    }
                    .verifyComplete()
            }
        }

        context("point보다 차감할 price가 더 높다면,") {

            val userId = idGenerator.generate()
            val price = 2_000L
            pointService.createPoint(userId).block()

            it("포인트를 차감하지 않고 IllegalArgumentException을 던진다.") {
                val result = pointService.payWithPoint(userId, price)

                StepVerifier.create(result)
                    .expectErrorMessage("Not enough point to pay point(\"1_000\") < price(\"2_000\")")
            }
        }
    }

    describe("exists 메소드는") {
        context("userId에 해당하는 Point가 이미 저장되어 있다면,") {

            val userId = idGenerator.generate()
            pointService.createPoint(userId).block()

            it("true를 반환한다.") {
                val result = pointService.exists(userId)

                StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete()
            }
        }

        context("userId에 해당하는 Point가 저장되어 있지 않다면,") {

            val userId = idGenerator.generate()

            it("false를 반환한다.") {
                val result = pointService.exists(userId)

                StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete()
            }
        }
    }

})
