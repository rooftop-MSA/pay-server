package org.rooftop.pay.domain

import org.springframework.context.event.EventListener
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
@Transactional(readOnly = true)
class PointService(
    private val idGenerator: IdGenerator,
    private val pointRepository: PointRepository,
) {

    @Transactional
    fun payWithPoint(userId: Long, price: Long): Mono<Point> {
        return pointRepository.findByUserId(userId)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists user \"$userId\"")
                }
            )
            .flatMap {
                it.pay(price)
                pointRepository.save(it)
            }
            .retryWhen(retryOptimisticLockingFailure)
    }

    @Transactional
    fun createPoint(userId: Long): Mono<Point> {
        return pointRepository.save(
            Point(
                id = idGenerator.generate(),
                userId = userId,
                point = NEW_USER_BONUS_POINT,
                isNew = true,
            )
        )
    }

    fun exists(userId: Long): Mono<Boolean> {
        return pointRepository.existsByUserId(userId)
    }

    @Transactional
    @EventListener(PointRollbackEvent::class)
    fun rollbackPoint(pointRollbackEvent: PointRollbackEvent): Mono<Point> {
        return pointRepository.findByUserId(pointRollbackEvent.userId)
            .flatMap {
                it.charge(pointRollbackEvent.paidPoint)
                pointRepository.save(it)
            }
            .retryWhen(retryOptimisticLockingFailure)
    }

    private companion object {
        private const val NEW_USER_BONUS_POINT = 1_000L
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val retryOptimisticLockingFailure =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
