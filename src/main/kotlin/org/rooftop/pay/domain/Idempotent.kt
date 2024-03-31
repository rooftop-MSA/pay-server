package org.rooftop.pay.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("idempotent")
class Idempotent(
    @Id
    @Column("id")
    val id: String,
)
