package org.rooftop.pay.domain

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("point")
class Point(
    @Id
    @Column("id")
    val id: Long,

    @Column("user_id")
    val userId: Long,

    @Column("point")
    var point: Long,

    @Version
    private var version: Int? = null,

    isNew: Boolean = false,

    createdAt: Instant? = null,

    modifiedAt: Instant? = null,
) : BaseEntity(createdAt, modifiedAt, isNew) {

    @PersistenceCreator
    private constructor(
        id: Long,
        userId: Long,
        point: Long,
        version: Int,
        createdAt: Instant,
        modifiedAt: Instant,
    ) : this(id, userId, point, version, false, createdAt, modifiedAt)

    override fun getId(): Long = id

    fun pay(price: Long) {
        require(point >= price) {
            "Not enough point to pay point(\"$point\") < price(\"$price\")"
        }
        this.point -= price
    }

    fun charge(point: Long) {
        this.point += point
    }

    override fun toString(): String {
        return "Point(id=$id, userId=$userId, point=$point, version=$version)"
    }


}
