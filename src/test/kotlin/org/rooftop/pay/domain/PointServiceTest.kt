package org.rooftop.pay.domain

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equality.shouldBeEqualToUsingFields
import org.rooftop.pay.infra.TsidGenerator
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration.Companion.seconds

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
    private val pointRepository: PointRepository,
    private val idGenerator: IdGenerator,
    private val applicationEventPublisher: ApplicationEventPublisher,
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

    describe("rollbackPoint 메소드는") {
        context("RollbackPointEvent 가 발행되면,") {
            val idempotentKey = UUID.randomUUID().toString()
            val userId = 20L
            val paidPoint = 500L

            pointService.createPoint(userId).subscribe()

            val expected = point(point = 1500L)

            it("point 를 롤백한다.") {
                pointService.rollbackPoint(idempotentKey, userId, paidPoint).subscribe()

                eventually(10.seconds) {
                    StepVerifier.create(pointRepository.findByUserId(userId))
                        .assertNext { it ->
                            it.shouldBeEqualToUsingFields(
                                expected,
                                Point::class.memberProperties.first { fields -> fields.name == "point" })
                        }
                }
            }
        }

        context("이미 rollback된적이 있다면,") {
            val idempotentKey = UUID.randomUUID().toString()
            val userId = 21L
            val paidPoint = 500L
            val expected = point(point = 1500L)

            pointService.createPoint(userId).subscribe()
            pointService.rollbackPoint(idempotentKey, userId, paidPoint).subscribe()

            it("point를 롤백하지 않는다.") {
                pointService.rollbackPoint(idempotentKey, userId, paidPoint).subscribe()

                eventually(10.seconds) {
                    StepVerifier.create(pointRepository.findByUserId(userId))
                        .assertNext {
                            it.shouldBeEqualToUsingFields(
                                expected,
                                Point::class.memberProperties.first { fields -> fields.name == "point" })
                        }
                }
            }
        }
    }

})
