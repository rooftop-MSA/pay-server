package org.rooftop.pay.integration

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.api.pay.payPointReq
import org.rooftop.api.pay.payRegisterOrderReq
import org.rooftop.netx.api.SagaManager
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.pay.Application
import org.rooftop.pay.app.RedisContainer
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.server.MockIdentityServer
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@EnableSaga
@AutoConfigureWebTestClient
@DisplayName("통합테스트의")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    classes = [
        Application::class,
        R2dbcConfigurer::class,
        MockIdentityServer::class,
        RedisContainer::class,
    ]
)
internal class IntegrationTest(
    private val api: WebTestClient,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
    private val mockIdentityServer: MockIdentityServer,
    private val sagaManager: SagaManager,
) : DescribeSpec({

    afterEach {
        r2dbcEntityTemplate.clearAll()
    }

    describe("createPay api는") {
        context("등록할 주문 정보와 transaction id를 전달받으면,") {
            val transactionId = sagaManager.start("").block()!!

            val payRegisterOrderReq = payRegisterOrderReq {
                this.orderId = 1L
                this.userId = USER_ID
                this.price = 1_000L
                this.transactionId = transactionId
            }

            it("결제를 대기상태로 생성한다.") {
                val result = api.createPay(payRegisterOrderReq)

                result.expectStatus().isOk
            }
        }
    }

    describe("payPoint api 는") {
        context("포인트가 충분한 유저가 결제를 요청할 경우,") {

            mockIdentityServer.enqueue200(userGetByTokenRes)

            val transactionId = sagaManager.start("").block()!!
            val payRegisterOrderReq = payRegisterOrderReq {
                this.orderId = ORDER_ID
                this.userId = USER_ID
                this.price = 1_000L
                this.transactionId = transactionId
            }
            api.createPay(payRegisterOrderReq)

            it("결제에 성공한다.") {
                val result = api.payPoint(VALID_TOKEN, payPointReq)

                result.expectStatus().isOk
            }
        }
    }
}) {

    companion object {
        private const val VALID_TOKEN = "VALID_TOKEN"
        private const val USER_ID = 2L
        private const val USER_NAME = "USER_NAME"
        private const val ORDER_ID = 3L

        private val userGetByTokenRes = userGetByTokenRes {
            this.id = USER_ID
            this.name = USER_NAME
        }

        private val payPointReq = payPointReq {
            this.orderId = ORDER_ID
        }
    }
}
