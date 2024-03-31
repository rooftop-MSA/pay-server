package org.rooftop.pay.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class PointService(
    private val idGenerator: IdGenerator,
    private val pointRepository: PointRepository,
    private val idempotentRepository: IdempotentRepository,
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

    fun exists(userId: Long): Mono<Boolean> {
        return pointRepository.existsByUserId(userId)
    }

    @Transactional
    fun rollbackPoint(idempotentKey: String, userId: Long, paidPoint: Long): Mono<Point> {
        return pointRepository.findByUserId(userId)
            .filterWhen {
                idempotentRepository.existsById(idempotentKey)
                    .map { it.not() }
            }
            .flatMap { point ->
                idempotentRepository.save(Idempotent(idempotentKey))
                    .map { point }
            }
            .flatMap {
                it.charge(paidPoint)
                pointRepository.save(it)
            }
    }

    private companion object {
        private const val NEW_USER_BONUS_POINT = 1_000L
    }
}
