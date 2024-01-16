package org.rooftop.pay.domain

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("payment")
class Payment(
    @Id
    @Column("id")
    val id: Long,

    @Column("user_id")
    val userId: Long,

    @Column("order_id")
    val orderId: Long,

    @Column("price")
    val price: Long,

    @Column("state")
    val state: PaymentState = PaymentState.PENDING,

    @Version
    private var version: Int? = null,

    isNew: Boolean = false,

    createdAt: Instant? = null,

    modifiedAt: Instant? = null,
) : BaseEntity(createdAt, modifiedAt, isNew) {

    @PersistenceCreator
    constructor(
        id: Long,
        userId: Long,
        orderId: Long,
        price: Long,
        state: PaymentState,
        version: Int,
        createdAt: Instant,
        modifiedAt: Instant,
    ) : this(id, userId, orderId, price, state, version, false, createdAt, modifiedAt)

    override fun getId(): Long = id

    fun fail(): Payment {
        return Payment(
            id,
            userId,
            orderId,
            price,
            PaymentState.FAILED,
            version!!,
            createdAt!!,
            modifiedAt!!
        )
    }
}
