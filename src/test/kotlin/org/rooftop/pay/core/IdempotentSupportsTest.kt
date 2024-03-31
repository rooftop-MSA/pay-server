package org.rooftop.pay.core

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.rooftop.pay.app.RedisContainer
import org.rooftop.pay.domain.R2dbcConfigurer
import org.rooftop.pay.infra.IdempotentConfigurer
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import java.util.*

@DataR2dbcTest
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        R2dbcConfigurer::class,
        IdempotentConfigurer::class,
        IdempotentCacheSupports::class,
    ]
)
@DisplayName("IdempotentCacheSupports 클래스의")
internal class IdempotentSupportsTest(
    private val idempotentCacheSupports: IdempotentCacheSupports,
) : DescribeSpec({

    describe("withIdempotent 메소드는") {
        context("이미 cache된 unique id가 있으면,") {
            val uniqueKey = UUID.randomUUID().toString()
            val idempotentFunc = idempotentCacheSupports.withIdempotent(uniqueKey) {
                Mono.fromCallable {
                    storage.add(uniqueKey)
                    uniqueKey
                }
            }

            it("로직을 실행하지 않는다.") {
                idempotentFunc.block()
                idempotentFunc.block()
                idempotentFunc.block()

                storage.size shouldBe 1

            }
        }
    }
}) {

    private companion object {
        val storage = mutableListOf<String>()
    }
}
