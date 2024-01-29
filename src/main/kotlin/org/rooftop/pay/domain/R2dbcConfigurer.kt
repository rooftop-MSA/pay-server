package org.rooftop.pay.domain

import org.rooftop.pay.Application
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackageClasses = [Application::class])
abstract class R2dbcConfigurer : AbstractR2dbcConfiguration()
