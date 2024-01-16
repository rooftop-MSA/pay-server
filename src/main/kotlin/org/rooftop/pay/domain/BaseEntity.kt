package org.rooftop.pay.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import java.time.Instant

abstract class BaseEntity(
    @CreatedDate
    @Column("created_at")
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column("modified_at")
    var modifiedAt: Instant? = null,

    @Transient
    private val isNew: Boolean = false,
) : Persistable<Long> {

    override fun isNew(): Boolean = isNew
}
