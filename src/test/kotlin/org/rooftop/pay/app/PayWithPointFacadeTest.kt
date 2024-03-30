package org.rooftop.pay.app

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.api.pay.payPointReq
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.pay.Application
import org.rooftop.pay.domain.PayService
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.server.MockIdentityServer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@EnableSaga
@SpringBootTest
@DisplayName("PayWithPointFacade 클래스의")
@ContextConfiguration(
    classes = [
        Application::class,
        R2dbcConfigurer::class,
        MockIdentityServer::class,
        RedisContainer::class,
    ]
)
internal class PayWithPointFacadeTest(
    private val payWithPointFacade: PayWithPointFacade,
    private val mockIdentityServer: MockIdentityServer,
    private val payService: PayService,
) : DescribeSpec({

    describe("payWithPoint 메소드는") {
        context("일치하는 유저의 payPointReq 를 받으면,") {

            mockIdentityServer.enqueue200(userGetByTokenRes)
            payService.createPayment(USER_ID, 2L, 1_000L).block()

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
    }
}
