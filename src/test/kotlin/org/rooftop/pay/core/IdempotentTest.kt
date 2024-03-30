package org.rooftop.pay.core

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.meta.EnableSaga
import org.rooftop.pay.app.RedisContainer
import org.rooftop.pay.infra.IdempotentConfigurer
import org.rooftop.pay.infra.RedisIdempotentCache
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import kotlin.time.Duration.Companion.seconds

@EnableSaga
@ContextConfiguration(
    classes = [
        RedisContainer::class,
        IdempotentConfigurer::class,
    ]
)
@DisplayName("Idempotent 클래스의")
@TestPropertySource("classpath:application.properties")
internal class IdempotentTest(
    private val idempotentFactory: IdempotentFactory,
) : DescribeSpec({

    describe("call 메소드는") {
        context("같은 uniqueId를 요청받으면,") {
            val idempotent =
                idempotentFactory.createWithRetryable<String, String>("idempotent test") { request ->
                    Mono.fromCallable {
                        storage.add(request)
                        request
                    }
                }

            val uniqueId = "1"

            it("멱등한 결과를 보장한다.") {
                repeat(10) {
                    idempotent.call(uniqueId, "A", String::class)
                        .onErrorResume {
                            it.printStackTrace()
                            Mono.empty()
                        }
                        .subscribe()
                }

                eventually(5.seconds) {
                    storage.size shouldBeEqual 1
                }
            }
        }
    }
}) {

    private companion object {
        private val storage = mutableListOf<String>()
    }
}
