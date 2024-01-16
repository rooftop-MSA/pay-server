package org.rooftop.pay.app

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.pay.payRegisterOrderReq
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.pay.Application
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.infra.transaction.RedisContainer
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@DisplayName("CreatePayFacade 클래스의")
@ContextConfiguration(
    classes = [
        Application::class,
        R2dbcConfigurer::class,
        RedisContainer::class,
        RedisAssertions::class
    ]
)
internal class CreatePayFacadeTest(
    private val createPayFacade: CreatePayFacade,
    private val redisAssertions: RedisAssertions,
) : DescribeSpec({

    describe("createPayment 메소드는") {
        context("상품을 생성하고,") {
            it("트랜잭션에 조인한다.") {
                createPayFacade.createPayment(payRegisterOrderReq).block()

                redisAssertions.assertUndoPaymentExist(payRegisterOrderReq.transactionId)
                redisAssertions.assertTransactionServer(
                    payRegisterOrderReq.transactionId,
                    joinTransaction
                )
            }
        }
    }
}) {

    private companion object {
        private val payRegisterOrderReq = payRegisterOrderReq {
            this.orderId = 1L
            this.userId = 2L
            this.transactionId = "123"
            this.price = 10_000L
        }

        private val joinTransaction = transaction {
            this.id = payRegisterOrderReq.transactionId
            this.serverId = "pay-1"
            this.state = TransactionState.TRANSACTION_STATE_JOIN
        }
    }
}
