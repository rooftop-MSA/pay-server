package org.rooftop.pay.domain

import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("idempotent")
class Idempotent(
    @Id
    @Column("id")
    private val id: String,
) : Persistable<String> {

    override fun getId(): String = id

    override fun isNew(): Boolean = true
}
