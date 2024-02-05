package org.rooftop.pay.app

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.api.pay.payPointReq
import org.rooftop.api.pay.payRegisterOrderReq
import org.rooftop.netx.redis.AutoConfigureRedisTransaction
import org.rooftop.pay.Application
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.server.MockIdentityServer
import org.rooftop.pay.server.MockOrderServer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@SpringBootTest
@DisplayName("PayWithPointFacade 클래스의")
@AutoConfigureRedisTransaction
@ContextConfiguration(
    classes = [
        Application::class,
        R2dbcConfigurer::class,
        MockIdentityServer::class,
        MockOrderServer::class,
        RedisContainer::class,
    ]
)
internal class PayWithPointFacadeTest(
    private val payWithPointFacade: PayWithPointFacade,
    private val mockOrderServer: MockOrderServer,
    private val mockIdentityServer: MockIdentityServer,
    private val payService: PayService,
) : DescribeSpec({

    describe("payWithPoint 메소드는") {
        context("일치하는 유저의 payPointReq 를 받으면,") {

            mockOrderServer.enqueue200()
            mockIdentityServer.enqueue200(userGetByTokenRes)
            payService.createPayment(price1000OrderReq).block()

            it("결제에 성공한다.") {
                val result = payWithPointFacade.payWithPoint(VALID_TOKEN, price1000PointReq)

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("주문에 저장된 userId와 구매자의 userId가 일치하지 않으면,") {

            val userGetByTokenRes = userGetByTokenRes {
                this.id = 2L
                this.name = USER_NAME
            }
            mockIdentityServer.enqueue200(userGetByTokenRes)

            it("구매에 실패하고 예외를 던진다.") {
                val result = payWithPointFacade.payWithPoint(INVALID_TOKEN, price1000PointReq)

                StepVerifier.create(result)
                    .verifyErrorMessage("Not matched userId \"2\"")
            }
        }

        context("유저가 갖고있는 포인트보다 주문의 가격이 더 비싸면,") {

            mockIdentityServer.enqueue200(userGetByTokenRes)
            payService.createPayment(price10000OrderReq).block()

            it("구매에 실패하고 예외를 던진다.") {
                val result = payWithPointFacade.payWithPoint(VALID_TOKEN, price10000PointReq)

                StepVerifier.create(result)
                    .verifyErrorMessage("Not enough point to pay point(\"0\") < price(\"10000\")")
            }
        }

        context("결제정보가 PENDING이 아니라면,") {

            val userGetByTokenRes = userGetByTokenRes {
                this.id = 3L
            }
            mockIdentityServer.enqueue200(userGetByTokenRes, userGetByTokenRes)
            mockOrderServer.enqueue200()

            val payRegisterOrderReq = payRegisterOrderReq {
                this.userId = 3L
                this.price = 500
                this.orderId = 10L
                this.transactionId = "10"
            }
            payService.createPayment(payRegisterOrderReq).block()

            val payPointReq = payPointReq {
                this.orderId = 10L
            }
            payWithPointFacade.payWithPoint(VALID_TOKEN, payPointReq).block()

            it("구매에 실패하고 예외를 던진다.") {
                val result = payWithPointFacade.payWithPoint(VALID_TOKEN, payPointReq)

                StepVerifier.create(result)
                    .verifyErrorMessage("Payment state can be changed to success when it is pending state.")
            }
        }
    }
}) {

    private companion object {
        private const val VALID_TOKEN = "TOKEN"
        private const val INVALID_TOKEN = "INVALID_TOKEN"
        private const val USER_ID = 1L
        private const val USER_NAME = "USER_NAME"

        private val userGetByTokenRes = userGetByTokenRes {
            this.id = USER_ID
            this.name = USER_NAME
        }

        private val price1000PointReq = payPointReq {
            this.orderId = 2L
        }

        private val price1000OrderReq = payRegisterOrderReq {
            this.userId = USER_ID
            this.orderId = 2L
            this.price = 1_000
            this.transactionId = "1"
        }

        private val price10000PointReq = payPointReq {
            this.orderId = 3L
        }

        private val price10000OrderReq = payRegisterOrderReq {
            this.userId = USER_ID
            this.orderId = 3L
            this.price = 10_000
            this.transactionId = "2"
        }
    }
}
