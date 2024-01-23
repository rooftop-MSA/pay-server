package org.rooftop.pay.domain

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface PointRepository : R2dbcRepository<Point, Long> {

    fun findByUserId(userId: Long): Mono<Point>

    fun existsByUserId(userId: Long): Mono<Boolean>
}
