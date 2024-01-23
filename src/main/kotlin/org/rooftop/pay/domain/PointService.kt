package org.rooftop.pay.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

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

    private companion object {
        private const val NEW_USER_BONUS_POINT = 1_000L
    }
}
