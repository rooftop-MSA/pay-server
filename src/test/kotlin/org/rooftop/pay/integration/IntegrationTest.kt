package org.rooftop.pay.integration

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.pay.PayPointReq
import org.rooftop.api.pay.payPointReq
import org.rooftop.api.pay.payRegisterOrderReq
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.pay.Application
import org.rooftop.pay.app.RedisAssertions
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.infra.transaction.RedisContainer
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@AutoConfigureWebTestClient
@DisplayName("통합테스트의")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    classes = [
        Application::class,
        RedisContainer::class,
        RedisAssertions::class,
        R2dbcConfigurer::class,
    ]
)
internal class IntegrationTest(
    private val api: WebTestClient,
    private val redisAssertions: RedisAssertions,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
) : DescribeSpec({

    afterEach {
        r2dbcEntityTemplate.clearAll()
    }

    describe("createPay api는") {
        context("등록할 주문 정보와 transaction id를 전달받으면, ") {
            it("결제를 대기상태로 생성한다.") {
                val result = api.createPay(payRegisterOrderReq)

                result.expectStatus().isOk
                redisAssertions.assertUndoPaymentExist(payRegisterOrderReq.transactionId)
                redisAssertions.assertTransactionServer(
                    payRegisterOrderReq.transactionId,
                    joinTransaction
                )
            }
        }
    }

    describe("payPoint api 는") {
        context("포인트가 충분한 유저가 결제를 요청할 경우,") {

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

        private val payRegisterOrderReq = payRegisterOrderReq {
            this.orderId = 1L
            this.userId = 2L
            this.price = 10_000L
            this.transactionId = "4971626623122412"
        }

        private val joinTransaction = transaction {
            this.id = payRegisterOrderReq.transactionId
            this.serverId = "pay-1"
            this.state = TransactionState.TRANSACTION_STATE_JOIN
        }

        private val payPointReq = payPointReq {
            this.orderId = payRegisterOrderReq.orderId
        }
    }
}
